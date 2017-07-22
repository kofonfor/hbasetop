import ratpack.exec.ExecController
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.service.StopEvent
import ratpack.exec.Execution
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import ratpack.http.client.HttpClient

class HBaseStatRetriever implements Service {
  def httpClient
  def executorService
  volatile ScheduledFuture<?> nextFuture
  volatile boolean stopped

  HBaseStatRetriever() {
    print "Retriever started\n"
  }

  void onStart(StartEvent event) throws Exception {
    httpClient = event.getRegistry().get(HttpClient.class)
    executorService = event.getRegistry().get(ExecController.class).getExecutor()
    stopped = false
    scheduleNext()
  }

  void onStop(StopEvent event) throws Exception {
    stopped = true
    Optional.ofNullable(nextFuture).ifPresent({ f -> f.cancel(true) })
  }

  def scheduleNext() {
    nextFuture = executorService.schedule(this.&run, 30, TimeUnit.SECONDS)
    print "scheduleNext finished!\n"
  }

  def run() {
    if (stopped) {
      return
    }

    try {
      Execution.fork()
        .onComplete({ scheduleNext() })
        .onError({ it.printStackTrace() })
        .start({
          httpClient.get(new URI("http://sverka-04:60010/table.jsp?name=invoices_v4")).then({ response ->
            System.out.println("Status: " + response.getStatusCode())
            parseStats(response.getBody().getText())
          })
        })
    } catch(Throwable ex) {
      println ex
    }
  }

  def parseStats(body) {
    body = (body =~ /(?s)<\?xml.*$/)[0]
    body = body.replaceAll(/<link[^>]*>/, "").replaceAll(/<meta[^>]*>/, "").replaceAll(/<input[^>]*>/, "").replaceAll(/&nbsp;/, "")
    def rootNode = new XmlParser().parseText(body)
    def divNode = rootNode.'**'.find { div ->
      div.attribute("class") == "container-fluid content"
    }.children().get(1)
    def tableNode = null
    for(int i=0; i<divNode.children().size(); i++) {
      println "Name: " + divNode.children().get(i).name().getLocalPart()
      if(divNode.children().get(i).name().getLocalPart() == "h2" && divNode.children().get(i).text() == "Table Regions") {
        tableNode = divNode.children().get(i+1)
        break
      }
    }
    tableNode.children().each {
      if(it.children().get(0).name().getLocalPart() == "td") {
        println "Region name: " + it.children().get(0).text()
        def shortName = (it.children().get(0).text() =~ "[^,]+,[^,]*,[^\\.]*\\.(.*)\\.")[0][1]
        println "Region short name: " + shortName
        println "Locality: " + it.children().get(4).text()
        println "Requests: " + it.children().get(5).text()
      }
    }
  }
}

import ratpack.groovy.template.MarkupTemplateModule

import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack
import ratpack.http.client.StreamedResponse
import ratpack.http.MutableHeaders
import ratpack.http.client.RequestSpec
import ratpack.http.client.HttpClient

ratpack {
  bindings {
    add new HBaseStatRetriever()
    module MarkupTemplateModule
  }

  handlers {
    get { HBaseStatRetriever retriever ->
      def rgns = retriever.getRegionsStat()
      render groovyMarkupTemplate("index.gtpl", regions: rgns)
    }

    get("test") { HttpClient httpClient ->
      httpClient.get(new URI("http://sverka-04:60010/table.jsp?name=invoices_v4")).then({ response ->
        render "Status: " + response.getStatusCode()
      })
    }

    files { dir "public" }
  }
}

yieldUnescaped '<!DOCTYPE html>'
html {
  head {
    meta(charset:'utf-8')
    title("HBase stat visualizer")

    meta(name: 'apple-mobile-web-app-title', content: 'HBase stat visualizer')
    meta(name: 'description', content: '')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1')

    link(href: '/images/favicon.ico', rel: 'shortcut icon')
  }
  body {
    table(border:1) {
      tr {
	th 'Name'
        th 'Total reqs'
        th '30-secs delta'
        th 'Locality'
        th 'Server'
      }
      regions.each {
        yieldUnescaped "<tr>"
          td it.key
          td it.value["requests"]
          td it.value["delta30"]
          td it.value["locality"]
          td it.value["server"]
        yieldUnescaped "</tr>"
      }
    }
  }
}

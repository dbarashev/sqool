package com.bardsoftware.sqool.contest

//import freemarker.cache.ClassTemplateLoader
//import io.ktor.application.Application
//import io.ktor.application.install
//import io.ktor.features.CallLogging
//import io.ktor.freemarker.FreeMarker
//import io.ktor.freemarker.FreeMarkerContent
//import io.ktor.pipeline.call
//import io.ktor.response.respond
//import io.ktor.routing.Routing
//import io.ktor.routing.get
//
//
//class App {}
///**
// * @author dbarashev@bardsoftware.com
// */
//fun Application.main() {
//  install(CallLogging)
//  install(FreeMarker) {
//    templateLoader = ClassTemplateLoader(App::class.java, "/tmpl/")
//  }
//  install(Routing) {
//    get("/") {
//      call.respond(FreeMarkerContent("index.ftl", emptyMap<String,String>()))
//    }
//  }
//}

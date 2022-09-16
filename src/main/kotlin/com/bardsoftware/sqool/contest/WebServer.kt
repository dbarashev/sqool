/*
 * Copyright (c) BarD Software s.r.o 2019
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

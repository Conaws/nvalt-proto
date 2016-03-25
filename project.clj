(defproject nvalt-proto "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure          "1.8.0-alpha2"] ; July 16th (Latest before hard-linking)
                 [re-frame                     "0.6.0"       ]
                 [datascript                   "0.13.3"      ] ; Latest (as of 1/2/2016)
                 ; MISCELLANEOUS
                 [com.taoensso/sente           "1.7.0"       ] ; Latest (as of 9/1/2016)
                 [org.clojure/core.async       "0.2.374"     ]
                 ; ===== FRONTEND =====
                 [org.clojure/clojurescript    "1.7.170"
                   :scope "provided"                         ]
                   ; UI
                   [reagent                    "0.5.1"
                     :exclusions [org.clojure/tools.reader]  ]
                   ; CSS
                   [garden                     "1.3.0"       ]
                   ; HTML  
                   [hiccup                     "1.0.5"       ]
                   ; DATA REPRESENTATION
                   [posh                       "0.3.3.1"     ]
                 ; ==== GRAPH ====
                   [aysylu/loom                "0.5.4"       ] ; Latest 1/26/2015
                 ; ==== PRINT ====
                   [fipp                       "0.6.4"       ] ; Latest (as of 1/2/2016)
                 ; ==== META ====
                   [org.clojure/tools.namespace "0.2.11"] ; Latest (as of 3/15/2016)
<<<<<<< HEAD
=======
                 ; HTTP
                   [cljs-http                   "0.1.39"      ]
>>>>>>> 1e6e88ffc53f7c6887f878881c0dc7c26cf9a06a
                 ]

  :plugins [[lein-environ        "1.0.1"      ]
            [lein-cljsbuild      "1.1.1"      ]
            [lein-asset-minifier "0.2.2"
             :exclusions [org.clojure/clojure]]
            [lein-shell          "0.5.0"      ]]

  :aliases {"compile-prod"  ["do" "clean," "cljsbuild" "once" "app"]}

  :min-lein-version "2.5.0"

  :uberjar-name "nvalt-proto.jar"

  #_(comment
    (do (require '[fipp.edn])
        (clojure.main/repl :print clojure.pprint/pprint)))

  :main nvalt_proto.core

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to ]]

  :source-paths   ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc"]
                             :compiler {:output-to    "target/cljsbuild/deploy/js/app.js"
                                        :output-dir   "target/cljsbuild/deploy/js/out"
                                        :asset-path   "js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {;:repl-options {:init-ns reagent1.repl}

                   :dependencies [[lein-figwheel               "0.5.0-2"
                                    :exclusions [org.clojure/core.memoize
                                                 ring/ring-core
                                                 org.clojure/clojure
                                                 org.ow2.asm/asm-all
                                                 org.clojure/data.priority-map
                                                 org.clojure/tools.reader
                                                 org.clojure/clojurescript
                                                 org.clojure/core.async
                                                 org.clojure/tools.analyzer.jvm]]
                                  [devcards          "0.2.1-6"
                                   :exclusions [org.clojure/tools.reader]]]


                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.5.0-2"
                              :exclusions [org.clojure/core.memoize
                                           ring/ring-core
                                           org.clojure/clojure
                                           org.ow2.asm/asm-all
                                           org.clojure/data.priority-map
                                           org.clojure/tools.reader
                                           org.clojure/clojurescript
                                           org.clojure/core.async
                                           org.clojure/tools.analyzer.jvm]]
                             [org.clojure/clojurescript "1.7.170"]]


                   :injections []

                   :figwheel {;:http-server-root "public"
                              :server-port      3450
                             ; :nrepl-port       7002
                              ;:nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]

                             ; :css-dirs         ["resources/public/css"]
                            ;  :ring-handler     nvalt-proto.handler/app
                            }

                   :env {:dev      true
                         :port     3500
                         :dat-host "localhost"}

                   :cljsbuild {:builds {:dev-app {:source-paths ["env/dev/cljs"]
                                                  :compiler {:main       "nvalt-proto.dev"
                                                             :source-map true}}

                                        :dev {:figwheel true
                                              :source-paths ["src/cljs" "src/cljc"
                                                             ]
                                              :compiler {:output-to            "resources/public/js/dev-compiled/dev.js"
                                                         :output-dir           "resources/public/js/dev-compiled/out"
                                                         :optimizations        :none
                                                         :main                 nvalt-proto.cards
                                                         :asset-path           "js/dev-compiled/out"
                                                         :source-map           true
                                                         :source-map-timestamp true
                                                         :cache-analysis       true}}
                                        :devcards-compile
                                             {:source-paths ["env/dev/cljs"]
                                              :compiler {:main         "nvalt-proto.cards"
                                                         :devcards     true
                                                         :output-to    "target/cljsbuild/public/js/devcards-compiled.js"
                                                         :output-dir   "target/cljsbuild/public/js/devcards-out"
                                                         :asset-path   "js/devcards-out"
                                                         :optimizations :advanced}}}}}

             :uberjar {:hooks       [minify-assets.plugin/hooks]
                       :prep-tasks  ["compile" ["cljsbuild" "once"]]
                       :env         {:production true
                                     :port       3000
                                     :dat-host   "datomicfree"}
                       :aot         :all
                       :omit-source true
                       :cljsbuild   {:jar true
                                     :builds {:app
                                              {:source-paths ["env/prod/cljs"]
                                               :compiler
                                               {:optimizations :advanced
                                                :pretty-print false
                                                :output-to    "target/cljsbuild/deploy/js/app.js"
                                                :output-dir   "target/cljsbuild/deploy/js/out"
                                                         }}}}}
                                                })

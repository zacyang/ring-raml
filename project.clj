(defproject ring-raml "0.1.0-SNAPSHOT"
  :description "A ring middleware validates request and response according to given RAML file"
  :url "https://github.com/zacyang/ring-raml"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [compojure "1.5.2"]
                 [raml-clj-parser "0.1.0"]
                 [cheshire "5.3.0"]
                 [com.github.fge/json-schema-validator "2.1.7"]])

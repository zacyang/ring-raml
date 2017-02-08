(ns ring-raml.core-test
  (:use compojure.core)
  (:require [clojure.test :refer :all]
            [compojure.route :as route]
            [ring-raml.core :refer :all]
            [cheshire.core :as cheshire]
            [ring.mock.request :as mock]
            [ring-raml.validator :as v]))

(defroutes app
  (GET "/url-get" {} {:body "get-content"})
  (route/not-found "Page not found"))

(defn parse-body [body]
  (cheshire/parse-string (slurp body) true))

(deftest test-valid-case
  (testing "should return response from app when valid "
    (with-redefs-fn {#'v/validate-req  (fn [req raml] req)
                     #'v/invalid-req?  (fn [req] false)
                     #'v/validate-resp (fn[req resp raml] resp)}

     #(let [ middleware-app (-> app raml-middleware)
            response        (middleware-app (-> (mock/request :get "/url-get")))
            body            (:body response)]
        (is (= (:status response) 200))
        (is (= (:body response) "get-content"))))))

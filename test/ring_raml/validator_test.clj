(ns ring-raml.validator-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [ring-raml.validator :as sut]
   [raml-clj-parser.core :as raml]))

(defn get-raml [path]
  (raml/read-raml path))

(deftest match-req
  (testing "req match return the raml section"
    (let [req              (-> (mock/request :get "/get-endpoint"))
          matched_raml_def {:description "Get relationships", :body {:application/json {:schema "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\",\"properties\":{\"familyid\":{\"type\":\"string\"},\"relationship\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"type\":{\"type\":\"string\"},\"id\":{\"type\":\"integer\"}},\"required\":[\"type\",\"id\"]}}},\"required\":[\"familyid\",\"relationship\"]}\n", :example "{\n  \"input\": \"s3://zencodertesting/test.mov\"\n}\n"}}}]
      (is (= (sut/match-req req (get-raml  "test/resources/sample.raml"))   matched_raml_def))))

  (testing "req with wrong method returns error info"
    (let [req        (-> (mock/request :get "/post-endpoint"))
          error_info {:ring-raml.validator/error (str "Request resource is not defined in raml spec " req)}]
      (is (= (sut/match-req req (get-raml  "test/resources/sample.raml")) error_info))))

  (testing "req with valid uri parameters"
    (let [req (mock/request :get "/users/3" )
          matched_raml_def
          {:response {"200\n" {:body {:application/text "bla"}}}}]

      (is (= (sut/match-req req (get-raml "test/resources/uri_parameters.raml")) matched_raml_def))))

  (testing "nested match"
    (let [req              (mock/request :get "/users/123/keys/456" )
          matched_raml_def {:response {"200\n" {:body {:application/text "bla"}}}} ]
      (is (= (sut/match-req req (get-raml "test/resources/uri_parameters.raml")) matched_raml_def)))))

(deftest valid-req
  (testing "valid req should pass transparently back"
    (let [req (-> (mock/request :get "/valid"))]

    ;;TODO:  (is (= (sut/validate-req req (get-raml)) req)))

    )))

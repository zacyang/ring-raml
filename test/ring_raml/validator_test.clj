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
          matched_raml_def {:description "Get relationships", :body {:application/json {:schema "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\",\"properties\":{\"familyid\":{\"type\":\"string\"},\"relationship\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"type\":{\"type\":\"string\"},\"id\":{\"type\":\"integer\"}},\"required\":[\"type\",\"id\"]}}},\"required\":[\"familyid\",\"relationship\"]}\n", :example "{\n  \"input\": \"s3://zencodertesting/test.mov\"\n}\n"}}}
          match_result     (sut/match-req req (get-raml  "test/resources/sample.raml"))]
      (is (= (::sut/raml_def match_result )   matched_raml_def))
      (is (= (::sut/error match_result) nil))
      (is (= (::sut/path match_result)) ["/get-endpoint" :get])))

  (testing "req with wrong method returns error info"
    (let [req        (-> (mock/request :get "/post-endpoint"))
          error_info {::sut/error (str "Requested resource is not defined in raml spec " req )}]
      (is (=  (sut/match-req req (get-raml  "test/resources/sample.raml")) error_info))))

  (testing "req with valid uri parameters"
    (let [req (mock/request :get "/users/3" )
          matched_raml_def
          {:response {"200\n" {:body {:application/text "bla"}}}}]

      (is (= (::sut/raml_def (sut/match-req req (get-raml "test/resources/uri_parameters.raml"))) matched_raml_def))))

  (testing "nested match"
    (let [req              (mock/request :get "/users/123/keys/456" )
          matched_raml_def {:response {"200\n" {:body {:application/text "bla"}}}}
          match_result (sut/match-req req (get-raml "test/resources/uri_parameters.raml"))]
      (is (= (::sut/raml_def match_result) matched_raml_def))
      (is (= (::sut/error match_result) nil))
      (is (= (::sut/path match_result) ["/users" "/{userId}" "/keys" "/{keyId}" :get])))))

(deftest valid-req
  ;;TODO WIP
  (testing "valid req should pass transparently back"
    (let [req (-> (mock/request :get "/valid"))
          raml (get-raml "test/resources/sample.raml")]
      (is (= req (sut/validate-req req  raml ))))))

(deftest invalid-req
  ;; (testing "req with invalid query parameter"
  ;;   (let [req        (mock/request :get "/users" "page=abc" )
  ;;         raml       (get-raml "test/resources/query_parameters.raml")
  ;;         error_info {:ring-raml.validator/error {:msg
  ;;                                                 (str "Invalid query parameter")
  ;;                                                 :path   []
  ;;                                                 :index  0
  ;;                                                 :type   :query-params
  ;;                                                 :expect {:type :integer}
  ;;                                                 :actual {:type  :string
  ;;                                                          :value "a"}}}]
  ;;     (is (= error_info (sut/validate-req req raml )))))

  (testing "req with invalid uri parameter")

  (testing "req missing header")

  (testing "req with invalid header value")

  (testing "req with content and header mismatch")

  (testing "req :post with invalid json"))

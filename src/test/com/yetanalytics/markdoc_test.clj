(ns com.yetanalytics.markdoc-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [com.yetanalytics.markdoc :as md]))

(deftest convert-md-test
  (testing "converts basic MD to HTML"
    (is (= "<h2 id=\"header\">Header</h2><p>Regular text</p>"
           (md/md->html "##Header\nRegular text"))))

  (testing "converts a link to HTML"
    (is (= "<p><a href='https://www.yetanalytics.com'>Link</a></p>"
           (md/md->html "[Link](https://www.yetanalytics.com)"))))

  (testing "converts a relative link to html extension"
    (is (= "<p><a href='another.html'>Link</a></p>"
           (md/md->html "[Link](another.md)"))))

  (testing "does not convert non-md relative links"
    (is (= "<p><a href='another.notmd'>Link</a></p>"
           (md/md->html "[Link](another.notmd)")))))


(deftest convert-dir-test
  (md/convert {:in-root "dev-resources/md-samples"
               :out-root "tmp/output"
               :template-file "dev-resources/template/sample.html.template"
               :template-vars {:custom "C8D470E5EB066B3671EF212BD61C19BC744FD92D"}})
  (let [output-1 (slurp "tmp/output/1.html")
        output-2 (slurp "tmp/output/2.html")]
    (testing "output files contain respective MD content"
      (is (clojure.string/includes? output-1
                                    "94672188B2C6B01FF0A6BF4CEAA781DD4C28E120"))
      (is (clojure.string/includes? output-2
                                    "E1D1BDB7F4C893FE656E27FB5A549CE37D8859DB")))
    (testing "template is included"
      (is (clojure.string/includes? output-1
                                    "0F2520DF2A2412F6B4A6CDCAB3B76FACCD24A496")))
    (testing "custom value was printed in template"
      (is (clojure.string/includes? output-1
                                    "C8D470E5EB066B3671EF212BD61C19BC744FD92D")))
    (testing "asset was copied"
      (is (clojure.string/includes? (slurp "tmp/output/assets/an-asset.txt")
                                    "90D8B1756D8566FC486C3F78B191969DB9ECE299")))))

(ns com.yetanalytics.markdoc
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as cstr]
            [markdown.core :as md]
            [markdown.transformers :as md-trans]
            [selmer.parser :as selm-parser]
            [clj-jgit.porcelain :refer [with-repo git-log]])
  (:import [java.io File]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Markdown content -> HTML content
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn md-ext->html-ext
  "Convert the `.md` extension of `file-path` to `.html`."
  [file-path]
  (cstr/replace file-path #"\.md" ".html"))

(def relative-md-path-re
  (re-pattern
   (str "\\("
        "(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//)" ; no web URLs
        "[A-Za-z0-9_\\-\\./]+"
        "\\.md"
        "(?:#[\\w\\?\\/\\\\\\:\\@\\.\\~\\!\\$\\&\\'\\(\\)\\*\\+\\,\\;\\=]*)?" ; fragments
        "\\)")))

(defn relative-md-links
  "Convert Markdown links in `text` into HTML links"
  [text state]
  [(cstr/replace text relative-md-path-re md-ext->html-ext)
   state])

(defn md->html
  "Wrapper for `md/md-to-html-string`."
  [text]
  (md/md-to-html-string
   text
   :heading-anchors true
   :replacement-transformers (cons relative-md-links
                                   md-trans/transformer-vector)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Git API for SHA
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Shut up VSCode warnings.
;; Unfortunately `with-repo` is a unique macro where you have a floating `repo`
;; symbol that is only bound after macroexpansion, so we cannot use `lint-as`
;; to lint it like a similar macro.
#_{:clj-kondo/ignore [:unresolved-symbol]}
(def git-sha
  (delay
    (try
      (with-repo "./"
        (-> (git-log repo :max-count 1)
            first
            :id
            (.getName)))
      (catch Exception _ "No SHA found."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Markdown file -> HTML file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def get-template
  (memoize (fn [template-file]
             (slurp template-file))))

(defn fill-template
  "Add `content` to the HTML doc template."
  [content template-file & {:keys [template-vars]}]
  (selm-parser/render (get-template template-file)
                      (merge template-vars
                             {:content content
                              :sha @git-sha})))

(defn all-paths-seq
  "Return a seq of all files located in `root`."
  [root]
  (->> (io/file root)
       file-seq
       (remove #(.isDirectory ^File %))
       (map #(.getPath ^File %))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Markdown doc folder -> HTML doc folder
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn convert
  "Given Markdown docs at `in-root`, create HTML docs in `out-root`."
  [{:keys [in-root out-root template-file template-vars]}]
  (assert (some? in-root) "You must provide a Markdown source directory :in-root")
  (assert (some? out-root) "You must provice a destination HTML directory :out-root")
  (let [in-root-regex (re-pattern (format "^%s" in-root))]
    (doseq [^String md-path (all-paths-seq in-root)
            :let [^String html-path (cstr/replace md-path
                                                  in-root-regex
                                                  out-root)]]
      ;; Make parent directory
      (io/make-parents html-path)
      ;; Make file
      (if (.endsWith md-path ".md")
        ;; Convert Markdown into HTML
        (let [doc-html (-> md-path slurp md->html)
              ;; Process with template if present
              html (cond-> doc-html
                     template-file (fill-template template-file
                                                  :template-vars template-vars))]
          (spit (io/file (md-ext->html-ext html-path)) html))
        ;; Simply copy other, non-HTML files (e.g. images)
        (io/copy (io/file md-path)
                 (io/file html-path))))))

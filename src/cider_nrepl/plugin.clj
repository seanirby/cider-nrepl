(ns cider-nrepl.plugin
  (:require [clojure.java.io :as io]
            [leiningen.core.main :as lein]))

;; Keep in sync with VERSION-FORM in project.clj
(defn- version
  []
  (let [v (-> (io/resource "cider/cider-nrepl/project.clj")
              slurp
              read-string
              (nth 2))]
    (assert (string? v)
            (str "Something went wrong, version is not a string: "
                 v))
    v))

(defn middleware
  [{:keys [dependencies] :as project}]
  (let [lein-version-ok? (lein/version-satisfies? (lein/leiningen-version) "2.5.2")
        clojure-version (->> dependencies
                             (some (fn [[id version & _]]
                                     (when (= id 'org.clojure/clojure)
                                       version))))
        clojure-version-ok? (if (nil? clojure-version)
                              ;; Lein 2.5.2+ uses Clojure 1.7 by default
                              lein-version-ok?
                              (lein/version-satisfies? clojure-version "1.7.0"))]

    (when-not lein-version-ok?
      (lein/warn "Warning: cider-nrepl requires Leiningen 2.5.2 or greater."))
    (when-not clojure-version-ok?
      (lein/warn "Warning: cider-nrepl requires Clojure 1.7 or greater."))
    (when-not (and lein-version-ok? clojure-version-ok?)
      (lein/warn "Warning: cider-nrepl will not be included in your project."))

    (cond-> project
      (and clojure-version-ok? lein-version-ok?)
      (-> (update-in [:dependencies]
                     (fnil into [])
                     [['cider/cider-nrepl (version)]])
          (update-in [:repl-options :nrepl-middleware]
                     (fnil into [])
                     (do (require 'cider.nrepl)
                         @(resolve 'cider.nrepl/cider-middleware)))))))

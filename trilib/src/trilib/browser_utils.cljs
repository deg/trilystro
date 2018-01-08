;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017-8, David Goldfarb

(ns trilib.browser-utils
  "Browser/chrome/closure utilities"
  (:require
   [clojure.spec.alpha :as s]
   [iron.closure-utils :refer [debug?]]
   [re-frisk.core :refer [enable-re-frisk!]]))


(enable-console-print!)

(defn dev-setup []
  (when debug?
    (enable-re-frisk!)
    (println "dev mode")))

;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.temp-utils
  "Immature utilities. Each should move from here once I'm comfortable with them"
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))


;;; [TODO] Move to iron.utils once this matures a bit
;;; [TODO] The URL is tainted text. Is there any risk here?
(defn link-to
  "Create an HTTP link. Use some smarts re user intention"
  [url-string]
  (when url-string
    (let [url (if (str/includes? url-string "/")
                url-string
                (str "http://" url-string))]
      [:a {:class "break-long-words" :href url} url-string])))



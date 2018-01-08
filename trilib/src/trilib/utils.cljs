;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017-8, David Goldfarb

(ns trilib.utils
  "Utilities that have no better home yet"
  (:require
   [clojure.spec.alpha :as s]
   [re-frame.loggers :refer [console]]))

(defn num-uuid
  "Short UUID for cases where a standard UUID is too long. (Sometimes
  for true efficiency concerns, but usually just to make some debug
  tracing less verbose)"
  []
  (str (rand-int 9999999999)))


(defonce app-name "VuAgain")

(defn pack-msg
  "Package a VuAgain message for sending between Chrome Extension
  background and foreground (content-script or popup)"
  [command msg]
  (clj->js (assoc msg :app app-name :command command)))

(defn unpack-msg
  [message case-handlers]
  (let [message (js->clj message :keywordize-keys true)]
    (if (and (map? message)
             (= (:app message) app-name))
      (case-handlers (:command message) message)
      (console :error "Received message in unknown format: " message))))

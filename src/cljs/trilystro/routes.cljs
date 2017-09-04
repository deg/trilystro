;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require
   [secretary.core :as secretary]
   [goog.events :as events]
   [goog.history.EventType :as EventType]
   [re-frame.core :as re-frame]
   [sodium.re-utils :refer [<sub >evt]]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")

  (defroute "/" []
    (>evt [:page :entry]))

  (defroute "/entry" []
    (>evt [:page :entry]))

  (defroute "/search" []
    (>evt [:page :search]))

  (defroute "/about" []
    (>evt [:page :about]))


  ;; --------------------
  (hook-browser-navigation!))


;;; [TODO] If we extend this at all, strongly consider replacing this ad-hoc code with
;;;        https://github.com/SMX-LTD/re-frame-document-fx
;;;        ALSO: In receipts app (also copy dev server logic from there)
(defn goto-page [page server]
  (aset js/window "location" (str "/#/" (name page) "?" #_(server-qp server))))


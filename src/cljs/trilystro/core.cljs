;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :refer [enable-re-frisk!]]
            [trilystro.events]
            [trilystro.subs]
            [trilystro.views :as views]
            [trilystro.config :as config]
            [trilystro.firebase :as fb]
            [trilystro.fsm :as fsm]))

(enable-console-print!)


(defn dev-setup []
  (when config/debug?
    (enable-re-frisk!)
    (println "dev mode")))


(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app-view]
                  (.getElementById js/document "app")))


(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch-sync [::fsm/goto :initialize-db])
  (dev-setup)
  (fb/init)
  (mount-root))


;;; This project assumes corresponding Firebase auth rules:
;;; See firebase-rules.json in the root of this project.

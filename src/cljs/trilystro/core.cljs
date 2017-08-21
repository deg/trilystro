;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :refer [enable-re-frisk!]]
            [trilystro.events]
            [trilystro.subs]
            [trilystro.routes :as routes]
            [trilystro.views :as views]
            [trilystro.config :as config]
            [trilystro.firebase :as firebase]))

(enable-console-print!)


;;; From https://console.firebase.google.com/u/0/project/trilystro/overview - "Add Firebase to your web app"
(defonce firebase-app-info
  #js{:apiKey "AIzaSyDlGqaASOVO2nqFGG35GiUjOgFF2vvntyk"
      :authDomain "trilystro.firebaseapp.com"
      :databaseURL "https://trilystro.firebaseio.com"
      :storageBucket "trilystro.appspot.com"})


(defn dev-setup []
  (when config/debug?
    (enable-re-frisk!)
    (println "dev mode")))


(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))


(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (firebase/init-fb firebase-app-info)
  (mount-root))

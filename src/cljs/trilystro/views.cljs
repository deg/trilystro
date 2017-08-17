(ns trilystro.views
  (:require
   [clojure.spec.alpha :as s]
   [re-frame.core :as re-frame]
   [sodium.core :as na]
   [sodium.extensions :as nax]
   [sodium.re-utils :refer [<sub >evt]]))


;; home

(defn home-panel []
  (fn []
    [na/container {}
       [nax/app-title [:name]]
       [na/form {}
        [na/form-button {:on-click (na/>event [:db-write :button-click])
                         :content "Write to DB"
                         :positive? true}]]
     [:div [:a {:href "#/about"} "go to About Page"]]]))


;; about

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a {:href "#/"} "go to Home Page"]]]))


;; main

(defn- panels [panel-name]
  (case panel-name
    :home-panel (home-panel)
    :about-panel [about-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [show-panel @active-panel])))

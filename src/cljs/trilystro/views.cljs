;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.views
  (:require
   [clojure.spec.alpha :as s]
   [com.degel.re-frame-firebase]
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [soda-ash.core :as sa]
   [sodium.core :as na]
   [sodium.extensions :as nax]
   [sodium.re-utils :refer [<sub >evt]]
   [sodium.utils :as utils]
   [trilystro.events :as events]
   [trilystro.routes :as routes]))


(defn home-panel []
  (let [text (reagent/atom "")
        new-key (reagent/atom "")]
    (fn []
      (let [all-keys (vals (<sub [:firebase/on-value {:path [:public :keywords]}]))
            guff (<sub [:firebase/on-value {:path [:public]}])]
        [na/container {}
         [:div (str "Got keys:" all-keys)]
         [na/form {}
          [na/input {:type :text
                     :on-change (na/>atom new-key)}]
          [na/text-area {:rows 3
                         :placeholder "the text"
                         :value @text
                         :on-change (na/>atom text)}]
          [na/form-button {:on-click (na/>event [:commit-lystro] {:text @text :keys [@new-key]})
                           :content "Save"
                           :positive? true}]]]))))

(defn about-panel []
  (let []
    [na/container {}
     [na/form {}
      [na/form-button {:on-click (na/>event [:db-write :button-click])
                       :content "Write to DB"
                       :positive? true}]]
     [:div "This is the About Page."]]))


(defn wrap-page [page]
  [na/container {}
   page])

(def tabs [{:id :home    :label "home"   :panel [wrap-page [home-panel]]}
           {:id :about   :label "about"  :panel [wrap-page [about-panel]]}])

(defn login-logout-control []
  (let [user (<sub [:user])]
    [na/menu-item {:active? false
                   :color "grey"
                   :position :right
                   :on-click (na/>event [(if user :sign-out :sign-in)])}
     (if user
       [na/label {:image true :circular? true}
        [na/image {:src (:photo-url user)}]
        (or (:displayName user) (:email user))]
       "login")]))


(defn tabs-row [& {:keys [tabs login-item]}]
  `[~@[na/menu {:tabular? true}]
    ~@(map (fn [{:keys [id label]}]
             (let [active? (= id (or (<sub [:page]) :home))
                   handler #(routes/goto-page id (<sub [:server]))]
               [na/menu-item {:name label
                              :active? active?
                              :color (if active? "blue" "grey")
                              :on-click handler}]))
           tabs)
    ~login-item])


(defn top-bar []
  [na/container {}
   [nax/app-title [:name]]
   [tabs-row :tabs tabs :login-item (login-logout-control)]])


(defn main-panel []
  (let [all-keys (vals (<sub [:firebase/on-value {:path [:public :keywords]}]))]
    [na/container {}
     [top-bar]
     (when-let [panel (<sub [:page])]
       (:panel (first (filter #(= (:id %) panel)
                              tabs))))]))

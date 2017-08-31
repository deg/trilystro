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


(defn keyword-selector []
  (let [old-keys (-> [:firebase/on-value {:path [:public :keywords]}]
                     <sub vals set)
        new-keys (<sub [:new-keys])]
    [:span [na/dropdown {:multiple? true
                         :button? true
                         :value     (<sub      [:form-state :entry [:selected-keys]] #{})
                         :on-change (na/>event [:form-state :entry [:selected-keys]] #{} set)
                         :options (na/dropdown-list (into old-keys new-keys) identity identity)}]
     [na/input {:type :text
                :placeholder "add key"
                :value     (<sub      [:form-state :entry [:new-key]] "")
                :on-change (na/>event [:form-state :entry [:new-key]])}]
     [na/button {:content "Add"
                 :on-click (na/>event [:add-new-key :entry])}]]))


(defn entry-panel []
  [na/form {:widths "equal"}
   [nax/labelled-field
    :label "keywords:"
    :inline? true
    :content [keyword-selector]]
   [nax/labelled-field
    :label "URL:"
    :inline? true
    :content [na/input {:type "url"
                        :placeholder "Website..."
                        :value     (<sub      [:form-state :entry [:url]] "")
                        :on-change (na/>event [:form-state :entry [:url]])}]]
   [nax/labelled-field
    :label "Text:"
    :content [na/text-area {:rows 3
                            :placeholder "Description..."
                            :value     (<sub      [:form-state :entry [:text]] "")
                            :on-change (na/>event [:form-state :entry [:text]])}]]


   [na/form-button {:on-click (na/>event [:commit-lystro :entry])
                    :content "Save"
                    :positive? true}]])


(defn about-panel []
  (let []
    [na/container {}
     [na/form {}]
     [:div "This is the About Page."]]))


(defn wrap-page [page]
  [na/container {} page])

(def tabs [{:id :entry    :label "New Lystro"   :panel [wrap-page [entry-panel]]}
           {:id :about   :label "about"  :panel [wrap-page [about-panel]]}])

(defn tabs-row [& {:keys [tabs login-item]}]
  `[~@[na/menu {:tabular? true}]
    ~@(map (fn [{:keys [id label]}]
             (let [active? (= id (or (<sub [:page]) :entry))
                   handler #(routes/goto-page id (<sub [:server]))]
               [na/menu-item {:name label
                              :active? active?
                              :color (if active? "blue" "grey")
                              :on-click handler}]))
           tabs)
    ~login-item])


(defn login-logout-control []
  (let [user (<sub [:user])]
    [na/menu {:vertical? true
              :secondary? true
              :fixed "right"}
     [na/menu-item {:on-click (na/>event [(if user :sign-out :sign-in)])}
      (if user
        [na/label {:image true :circular? true}
         [na/image {:src (:photo-url user)}]
         (or (:display-name user) (:email user))]
        "login...")]]))


(defn main-panel []
  (let [all-keys (vals (<sub [:firebase/on-value {:path [:public :keywords]}]))]
    [na/container {}
     [na/container {}
      (login-logout-control)
      [nax/app-title [:name]]
      [tabs-row :tabs tabs]]
     (when-let [panel (<sub [:page])]
       (:panel (first (filter #(= (:id %) panel)
                              tabs))))]))

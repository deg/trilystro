;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.views
  (:require
   [clojure.spec.alpha :as s]
   [clojure.set :as set]
   [clojure.string :as str]
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


(defn keyword-selector [form {:keys [allow-new?]}]
  (let [old-keys (-> [:firebase/on-value {:path [:public :keywords]}]
                     <sub vals set)
        new-keys (<sub [:new-keys])]
    [:span [na/dropdown {:multiple? true
                         :button? true
                         :value     (<sub      [:form-state form [:selected-keys]] #{})
                         :on-change (na/>event [:form-state form [:selected-keys]] #{} set)
                         :options (na/dropdown-list (into old-keys new-keys) identity identity)}]
     (when allow-new?
       [:span
        [na/input {:type :text
                   :placeholder "add key"
                   :value     (<sub      [:form-state form [:new-key]] "")
                   :on-change (na/>event [:form-state form [:new-key]])}]
        [na/button {:content "Add"
                    :on-click (na/>event [:add-new-key form])}]])]))


(defn entry-panel []
  [na/form {:widths "equal"}
   [nax/labelled-field
    :label "keywords:"
    :inline? true
    :content [keyword-selector :entry {:allow-new? true}]]
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

(defn lystro-grid [keys url text]
  (let [params1 {:width 3 :text-align "right"}
        params2 {:width 13}]
    [na/container {}
     [na/grid {:celled? true}
      [na/grid-row {}
       [na/grid-column params1 "Keywords:"]
       [na/grid-column params2 keys]]
      [na/grid-row {}
       [na/grid-column params1 "URL:"]
       [na/grid-column params2 url]]
      [na/grid-row {}
       [na/grid-column params1 "Text:"]
       [na/grid-column params2 text]]]]))

(defn lystro [{:keys [keys text url]}]
  (lystro-grid (into [na/list-na {:horizontal? true}]
                     (map (fn [key] [na/list-item {} key]) keys))
               url
               text))

(defn filter-some-keys [candidates match-set]
  {:pre [(utils/validate set? match-set)]}
  (if (empty? match-set)
    candidates
    (filter (fn [{:keys [keys]}]
              (-> (set keys)
                  (set/intersection match-set)
                  empty?
                  not))
            candidates)))

(defn filter-text [candidates match-text]
  {:pre [(utils/validate string? match-text)]}
  (if (empty? match-text)
    candidates
    (filter (fn [{:keys [text] :or {text ""}}]
              (str/includes? text match-text))
            candidates)))

(defn filter-url [candidates match-url]
  {:pre [(utils/validate string? match-url)]}
  (if (empty? match-url)
    candidates
    (filter (fn [{:keys [url] :or {url ""}}]
              (str/includes? url match-url))
            candidates)))


(defn search-panel []
  (fn []
    (let [all-keys (vals (<sub [:firebase/on-value {:path (events/public-fb-path [:keywords])}]))
          selected-keys (set (<sub [:form-state :search [:selected-keys]]))
          selected-url (<sub [:form-state :search [:url]] "")
          selected-text (<sub [:form-state :search [:text]] "")
          all-lystros (vals (<sub [:firebase/on-value {:path (events/private-fb-path [:items])}]))]
      [na/form {:widths "equal"}
       [nax/panel-header "Query"]
       [lystro-grid [keyword-selector :search {:allow-new? false}]
        [na/input {:type "url"
                   :placeholder "Website..."
                   :value     (<sub      [:form-state :search [:url]] "")
                   :on-change (na/>event [:form-state :search [:url]])}]
        [na/text-area {:rows 3
                       :placeholder "Description..."
                       :value     (<sub      [:form-state :search [:text]] "")
                       :on-change (na/>event [:form-state :search [:text]])}]]
       [nax/panel-header "Results"]
       (into [na/container {}]
             (map lystro
                  (-> all-lystros
                      (filter-some-keys selected-keys)
                      (filter-url selected-url)
                      (filter-text selected-text))))])))


(defn about-panel []
  [na/container {}
     [na/form {}]
   [:div "This is the About Page."]])


(defn wrap-page [page]
  [na/container {} page])

(def tabs [{:id :entry    :label "New Lystro" :panel [wrap-page [entry-panel]]}
           {:id :search   :label "Search"     :panel [wrap-page [search-panel]]}
           {:id :about    :label "about"      :panel [wrap-page [about-panel]]}])

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
  [na/container {}
   (login-logout-control)
   [nax/app-header [:name]]
   ;; [TODO] Should offer a logged-in event, like events/logged-in, for clarity
   (if-let [uid (<sub [:uid])]
     (let [all-keys (vals (<sub [:firebase/on-value {:path (events/public-fb-path [:keywords])}]))
           all-lystros (vals (<sub [:firebase/on-value {:path (events/private-fb-path [:items])}]))]
       [na/container {}
        [tabs-row :tabs tabs]
        (when-let [panel (<sub [:page])]
          (:panel (first (filter #(= (:id %) panel)
                                 tabs))))])
     "Not logged in")])

;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.views
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [com.degel.re-frame-firebase]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [reagent.core :as reagent]
   [soda-ash.core :as sa]
   [sodium.core :as na]
   [sodium.extensions :as nax]
   [sodium.re-utils :refer [<sub >evt]]
   [sodium.utils :as utils]
   [trilystro.events :as events]
   [trilystro.routes :as routes]))


(defn keyword-selector [form {:keys [allow-new?]}]
  (let [old-keys (-> [:firebase/on-value {:path (events/public-fb-path [:keywords])}]
                     <sub vals set)
        new-keys (<sub [:new-keys])]
    [:span
     [na/dropdown {:multiple? true
                   :button? true
                   :value     (<sub      [:form-state form [:keys]] #{})
                   :on-change (na/>event [:form-state form [:keys]] #{} set)
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
                        :default-value (<sub [:form-state :entry [:url]] "")
                        :on-change (na/>event [:form-state :entry [:url]])}]]
   [nax/labelled-field
    :label "Text:"
    :content [na/text-area {:rows 3
                            :placeholder "Description..."
                            :default-value (<sub [:form-state :entry [:text]] "")
                            :on-change (na/>event [:form-state :entry [:text]])}]]
   [na/form-button {:on-click (na/>event [:commit-lystro :entry])
                    :content "Save"
                    :positive? true}]])

;;; [TODO] Move to sodium.utils once this matures a bit
;;; [TODO] The URL is tainted text. Is there any risk here?
(defn link-to
  "Create an HTTP link. Use some smarts re user intention"
  [url-string]
  (let [url (if (str/includes? url-string "/")
              url-string
              (str "http://" url-string))]
    [:a {:href url} url-string]))

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
       [na/grid-column params2 (link-to url)]]
      [na/grid-row {}
       [na/grid-column params1 "Text:"]
       [na/grid-column params2 text]]]]))

(defn lystro-panel [{:keys [keys text url] :as lystro}]
  [na/container {}
   [lystro-grid
    `[~na/list-na {:horizontal? true}
      ~@(map (fn [key] [na/list-item {} key]) keys)]
    url
    text]
   [na/button {:content "edit"
               :color "teal"
               :size "tiny"
               :on-click (na/>event [:form-state :entry nil (assoc lystro :editing true)])}]
   [na/button {:content "delete"
               :color "red"
               :size "tiny"
               :on-click (na/>event [:clear-lystro (:firebase-id lystro)])}]])


(defn main-panel []
  (fn []
    (let [selected-keys (set (<sub [:form-state :search [:keys]]))
          selected-url (<sub [:form-state :search [:url]])
          selected-text (<sub [:form-state :search [:text]])
          keys-mode (<sub [:form-state :search [:keys-mode]])
          lystros (<sub [:lystros {:keys-mode keys-mode :keys selected-keys :url selected-url :text selected-text}])]
      [na/form {:widths "equal"}
       [na/modal {:open? (<sub [:form-state :entry [:editing]])
                  :dimmer "blurring"
                  :close-icon true
                  :on-close (na/>event [:form-state :entry [:editing] false])}
        [na/modal-header {} "Editing"]
        [na/modal-content {} [entry-panel]]]
       [nax/panel-header "Add Lystro"]
       [na/button {:content "New Lystro"
                   :color "teal"
                   :size "tiny"
                   :on-click (na/>event [:form-state :entry [:editing] true])}]
       [nax/panel-header "Search Lystros"]
       [lystro-grid
        [na/container {}
         [na/dropdown {:inline? true
                       :value     (<sub      [:form-state :search [:keys-mode]] :any-of)
                       :on-change (na/>event [:form-state :search [:keys-mode]] :any-of keyword)
                       :options (na/dropdown-list [[:all-of "All of"] [:any-of "Any of"]] first second)}]
         [keyword-selector :search {:allow-new? false}]]
        [na/input {:type "url"
                   :placeholder "Website..."
                   :value     (<sub      [:form-state :search [:url]] "")
                   :on-change (na/>event [:form-state :search [:url]])}]
        [na/text-area {:rows 3
                       :placeholder "Description..."
                       :value     (<sub      [:form-state :search [:text]] "")
                       :on-change (na/>event [:form-state :search [:text]])}]]
       [nax/panel-subheader "Results"]
       `[~na/container {}
         ~@(mapv lystro-panel lystros)]])))


(defn about-panel []
  [na/modal {:open? true
             :dimmer "blurring"
             :close-icon true
             :close-on-dimmer-click? false
             :on-close #(routes/goto-page :main nil)}
   [na/modal-header {}
    (str "About " (<sub [:name]))]
   [na/modal-content {}
    [:div "Trilystro is still a toy app, playing with ideas about Firebase and data curation."]
    [:div "Copyright (c) 2017, David Goldfarb (deg@degel.com)"]]])


(defn wrap-page [page]
  [na/container {} page])

(def tabs [{:id :main  :label "Main"  :panel (wrap-page [main-panel])}
           {:id :about :label "about" :panel (wrap-page [about-panel])}])

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

(defn top-bar []
  [na/container {}
   [login-logout-control]
   [nax/app-header [:name]]])


;;; We want to keep the Firebase ":on" subscriptions active, so need to mount them in the
;;; main panel. But, we don't want anything to show. We could use a display:none div, but
;;; this head-fake is more elegant, and seems to work works.
;;; See discussion in Slack #clojurescript channel Sept 6-7 2017.
(defn ^:export null-op [x] "")

(defn app-view []
  (if-let [uid (<sub [:uid])]
    (let [all-keys (re-frame/subscribe [:firebase/on-value {:path (events/public-fb-path [:keywords])}])
          all-lystros (re-frame/subscribe [:firebase/on-value {:path (events/private-fb-path [:items])}])]
      [na/container {}
       (list (null-op @all-lystros) (null-op @all-keys))
       [top-bar]
       [tabs-row :tabs tabs]
       (when-let [panel (<sub [:page])]
         (:panel (first (filter #(= (:id %) panel)
                                tabs))))])
    [na/container {}
     [top-bar]
     "Not logged in"]))

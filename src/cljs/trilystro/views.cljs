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
   [trilystro.firebase :as fb]
   [trilystro.fsm :as fsm]))


(defn keyword-selector [form {:keys [allow-new?]}]
  (let [old-tags (-> [:firebase/on-value {:path (fb/public-fb-path [:tags])}]
                     <sub vals set)
        new-tags (<sub [:new-tags])]
    [:span
     [na/dropdown {:multiple? true
                   :button? true
                   :value     (<sub      [:form-state form [:tags]] #{})
                   :on-change (na/>event [:form-state form [:tags]] #{} set)
                   :options (na/dropdown-list (into old-tags new-tags) identity identity)}]
     (when allow-new?
       [:span
        [na/input {:type :text
                   :placeholder "add tag"
                   :value     (<sub      [:form-state form [:new-tag]] "")
                   :on-change (na/>event [:form-state form [:new-tag]])}]
        [na/button {:content "Add"
                    :on-click (na/>event [:add-new-tag form])}]])]))


(defn entry-panel []
  [na/form {:widths "equal"}
   [nax/labelled-field
    :label "Tags:"
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
   [na/form-button {:on-click (na/>events [[[:commit-lystro :entry]]
                                           [[:page :quit]]])
                    :content "Save"
                    :positive? true}]])

(defn modal-entry-panel []
  (let [new?  (<sub [:in-page :modal-new-lystro])
        edit? (<sub [:in-page :modal-edit-lystro])]
    [na/modal {:open? (or new? edit?)
               :dimmer "blurring"
               :close-icon true
               :on-close (na/>event [:page :quit])}
     [na/modal-header {} (cond new? "Add Lystro"
                               edit? "Edit Lystro"
                               :default "???")]
     [na/modal-content {} [entry-panel]]]))


;;; [TODO] Move to sodium.utils once this matures a bit
;;; [TODO] The URL is tainted text. Is there any risk here?
(defn link-to
  "Create an HTTP link. Use some smarts re user intention"
  [url-string]
  (let [url (if (str/includes? url-string "/")
              url-string
              (str "http://" url-string))]
    [:a {:href url} url-string]))


(defn lystro-grid [params tags url text]
  (let [row-params {:color (or (:color params) "grey")}
        label-params {:width 3 :text-align "right"}
        value-params {:width 13}]
    [na/grid {:celled? true}
     [na/grid-row row-params
       [na/grid-column label-params "Tags:"]
       [na/grid-column value-params tags]]
      [na/grid-row row-params
       [na/grid-column label-params "URL:"]
       [na/grid-column value-params (if (string? url) (link-to url) url)]]
      [na/grid-row row-params
       [na/grid-column label-params "Text:"]
       [na/grid-column value-params text]]] ))

(defn lystro-panel [{:keys [tags text url] :as lystro}]
  [:div
   [lystro-grid {:color "black"}
    `[~na/list-na {:horizontal? true}
      ~@(map (fn [tag] [na/list-item {} tag]) tags)]
    url
    text]
   [na/button {:content "edit"
               :color "teal"
               :size "tiny"
               :on-click (na/>events [[[:form-state :entry nil lystro]]
                                      [[:page :modal-edit-lystro]]])}]
   [na/button {:content "delete"
               :color "red"
               :size "tiny"
               :on-click (na/>event [:clear-lystro (:firebase-id lystro)])}]])


(defn main-panel []
  (fn []
    (let [selected-tags (set (<sub [:form-state :search [:tags]]))
          selected-url (<sub [:form-state :search [:url]])
          selected-text (<sub [:form-state :search [:text]])
          tags-mode (<sub [:form-state :search [:tags-mode]])
          lystros (<sub [:lystros {:tags-mode tags-mode :tags selected-tags :url selected-url :text selected-text}])]
      [na/form {:widths "equal"}
       [nax/panel-header "Search Lystros"]
       [lystro-grid {:color "purple"}
        [na/container {}
         [na/dropdown {:inline? true
                       :value     (<sub      [:form-state :search [:tags-mode]] :any-of)
                       :on-change (na/>event [:form-state :search [:tags-mode]] :any-of keyword)
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
       `[:div {}
         ~@(mapv lystro-panel lystros)]])))


(defn about-panel []
  [na/container {}
   [:div "Trilystro is still a toy app, playing with ideas about Firebase and data curation."]
   [:div "Copyright (c) 2017, David Goldfarb (deg@degel.com)"]
   (fsm/render-graph fsm/page-states)])

(defn modal-about-panel []
  [na/modal {:open? (<sub [:in-page :modal-about])
             :dimmer "blurring"
             :close-icon true
             :close-on-dimmer-click? false
             :on-close (na/>event [:page :quit])}
   [na/modal-header {}
    (str "About " (<sub [:name]))]
   [na/modal-content {}
    [about-panel]]])


(defn login-logout-control []
  (let [user (<sub [:user])]
    [sa/MenuMenu {:position "right"}
     [na/menu-item {:on-click (na/>event [(if user :sign-out :sign-in)])}
      (if user
        [na/label {:image true :circular? true}
         [na/image {:src (:photo-url user)}]
         (or (:display-name user) (:email user))]
        "login...")]] ))

(defn top-bar []
  [na/menu {:fixed "top"}
   [na/menu-item {:header? true
                  :on-click (na/>event [:page :modal-about])}
    [sa/Icon {:name "eye" :size "big"}]
    (<sub [:name])]
   [na/menu-item {:name "Add"
                  :disabled? (not (<sub [:in-page :logged-in]))
                  :on-click (na/>events [[[:form-state :entry nil nil]]
                                         [[:page :modal-new-lystro]]])}]
   [na/menu-item {:name "About"
                  :on-click (na/>event [:page :modal-about])}]
   [login-logout-control]])


;;; We want to keep the Firebase ":on" subscriptions active, so need to mount them in the
;;; main panel. But, we don't want anything to show. We could use a display:none div, but
;;; this head-fake is more elegant, and seems to work works.
;;; [TODO] ^:export is probably not needed, but I've not tested removing it. See
;;;        discussion in Slack #clojurescript channel Sept 6-7 2017.
(defn ^:export null-op [x] "")

(defn app-view []
  [na/container {}
   [modal-about-panel]
   [top-bar]
   (when (<sub [:in-page :logged-in])
     (let [all-tags (re-frame/subscribe [:firebase/on-value {:path (fb/public-fb-path [:tags])}])
           all-lystros (re-frame/subscribe [:firebase/on-value {:path (fb/private-fb-path [:items])}])]
       [na/container {:style {:margin-top "5em"}}
        [modal-entry-panel]
        (list (null-op @all-lystros) (null-op @all-tags))
        [main-panel]]))] )

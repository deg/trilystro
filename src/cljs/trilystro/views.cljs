;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns trilystro.views
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [com.degel.re-frame-firebase]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [sodium.core :as na]
   [sodium.extensions :as nax]
   [sodium.utils :as utils]
   [sodium.re-utils :refer [<sub >evt]]
   [trilystro.config :as config]
   [trilystro.events :as events]
   [trilystro.firebase :as fb]
   [trilystro.fsm :as fsm]
   [trilystro.temp-utils :as tmp-utils]
   [trilystro.view-modal-about :as v-about]
   [trilystro.view-modal-confirm-delete :as v-confirm-delete]
   [trilystro.view-modal-show-exports :as v-show-exports]
   [trilystro.view-modal-entry :as v-entry]))


(defn lystro-search-grid
  "Grid to show Lystro elements.
  [TODO] Currently used only for the search box. Cleanup may be due"
  [params tags url text]
  (let [row-params {:color (or (:color params) "grey")}
        label-params {:width 3 :text-align "right"}
        value-params {:width 13}]
    [na/grid {:celled? true}
     [na/grid-row row-params
       [na/grid-column label-params "Tags:"]
       [na/grid-column value-params tags]]
      [na/grid-row row-params
       [na/grid-column label-params "URL:"]
       [na/grid-column value-params (if (string? url) (tmp-utils/link-to url) url)]]
      [na/grid-row row-params
       [na/grid-column label-params "Text:"]
       [na/grid-column value-params text]]]))


(defn search-panel
  "Component to specify Lystro search terms"
  []
  [lystro-search-grid {:color "brown"}
   [na/container {}
    [na/dropdown {:inline? true
                  :value     (<sub      [::fsm/page-param-val        :tags-mode] :any-of)
                  :on-change (na/>event [::fsm/update-page-param-val :tags-mode] :any-of keyword)
                  :options (na/dropdown-list [[:all-of "All of"] [:any-of "Any of"]] first second)}]
    [nax/tag-selector {:all-tags-sub            [:all-tags]
                       :selected-tags-sub       [::fsm/page-param-val :tags]
                       :set-selected-tags-event [::fsm/update-page-param-val :tags]}]]
   [na/input {:type "url"
              :placeholder "Website..."
              :value     (<sub      [::fsm/page-param-val :url] "")
              :on-change (na/>event [::fsm/update-page-param-val :url])}]
   [na/text-area {:rows 3
                  :placeholder "Description..."
                  :value     (<sub      [::fsm/page-param-val :text] "")
                  :on-change (na/>event [::fsm/update-page-param-val :text])}]])


(defn mini-button
  "Icon-only button, good for standard actions"
  [icon handler]
  [na/button {:icon icon
              :floated "right"
              :color "brown"
              :size "mini"
              :on-click handler}])


(defn lystro-results-panel
  "Render one Lystro"
  [{:keys [tags text url owner public?] :as lystro}]
  (let [mine? (= owner (<sub [:uid]))]
    [na/segment {:secondary? (not mine?)
                 :tertiary? (not public?)
                 :class-name "lystro-result"}
     (when mine? (mini-button "delete" (na/>event [::fsm/goto :modal-confirm-delete {:param lystro}])))
     (when mine? (mini-button "write"  (na/>event [::fsm/goto :modal-edit-lystro    {:param lystro}])))
     [nax/draw-tags {:selected-tags-sub       [::fsm/page-param-val :tags]
                     :set-selected-tags-event [::fsm/update-page-param-val :tags]
                     :class-of-tag-sub        [:tag-class-by-frequency]}
      tags]
     [:div {:on-click #(when mine?
                         (>evt [::fsm/goto :modal-edit-lystro {:param lystro}]))
            :class-name (str "text break-long-words "
                             (if mine? "editable-text" "frozen-text"))}
      text]
     [:div {:class-name "url"}
      (tmp-utils/link-to url)]
     (when (not mine?)
       [:div {:class "owner-sig"}
        (<sub [:user-pretty-name owner])])]))


(defn main-panel
  "The main screen"
  []
  [na/form {}
   [na/divider {:horizontal? true :section? true} "Search Lystros"]
   [search-panel]
   (let [selected-lystros
         (<sub [:lystros {:tags-mode (<sub [::fsm/page-param-val :tags-mode])
                          :tags      (<sub [::fsm/page-param-val :tags])
                          :url       (<sub [::fsm/page-param-val :url])
                          :text      (<sub [::fsm/page-param-val :text])}])]
     [:div
      [na/divider {:horizontal? true :section? true}
       (str "Results (" (count selected-lystros) ")")]
      `[:div {}
        ~@(mapv lystro-results-panel (utils/ci-sort-by :text selected-lystros))]
      [na/divider {:horizontal? true :section? true}]
      [na/container {}
       [na/button {:size "mini"
                   :icon "external share"
                   :content "export all"
                   :on-click #(>evt [::fsm/goto :modal-show-exports {:param (<sub [:lystros])}])}]
       [na/button {:size "mini"
                   :icon "share"
                   :content "export current"
                   :on-click #(>evt [::fsm/goto :modal-show-exports {:param selected-lystros}])}]]])])


(defn login-logout-control []
  (let [user (<sub [:user])]
    [na/menu-menu {:position "right"}
     [na/menu-item {:on-click (na/>event [(if user :sign-out :sign-in)])}
      (if user
        [na/label {:image true :circular? true}
         [na/image {:src (:photo-url user)}]
         (or (:display-name user) (:email user))]
        "login...")]
     (let [connected? (:firebase/connected? (<sub [:firebase/connection-state]))]
       [na/menu-item {:icon (if connected? "signal" "wait")
                      :content (if connected? "online" "offline")}])] ))

(defn top-bar []
  [na/menu {:fixed "top"}
   [na/menu-item {:header? true
                  :on-click (na/>event [::fsm/goto :modal-about])}
    [na/icon {:name "tasks" :size "big"}]
    (<sub [:name])]
   [na/menu-item {:name "Add"
                  :disabled? (not (<sub [::fsm/in-page? :logged-in]))
                  :on-click (na/>event [::fsm/goto :modal-new-lystro])}]
   [na/menu-item {:name "About"
                  :on-click (na/>event [::fsm/goto :modal-about])}]
   [login-logout-control]])



;;; We want to keep the Firebase ":on" subscriptions active, so need to mount them in the
;;; main panel. But, we don't want anything to show. We could use a display:none div, but
;;; this head-fake is more elegant, and seems to work works.
;;; [TODO] ^:export is probably not needed, but I've not tested removing it. See
;;;        discussion in Slack #clojurescript channel Sept 6-7 2017.
(defn ^:export null-op [x] "")

(defn app-view []
  [na/container {}
   [v-about/modal-about-panel]
   [v-entry/modal-entry-panel]
   [v-confirm-delete/modal-confirm-delete]
   [v-show-exports/modal-show-exports]
   [top-bar]
   [na/container {:style {:margin-top "5em"}}
    [nax/google-ad
     :unit "half banner"
     :ad-client "ca-pub-7080962590442738"
     :ad-slot "5313065038"
     :test (when config/debug? "... ADVERT  HERE ...")]
    (when (<sub [::fsm/in-page? :logged-in])
      (let [open-state ;; Subs that should be held open for efficiency
            [(<sub [:firebase/on-value {:path (fb/private-fb-path [:lystros])}])
             (<sub [:firebase/on-value {:path (fb/private-fb-path [:user-settings])}])
             (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:lystros])}])
             (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:user-details])}]) ;; [TODO][ch94] rename
             ]]
        (null-op open-state)
        [main-panel]))]])

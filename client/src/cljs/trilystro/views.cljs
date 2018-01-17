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
   [iron.re-utils :refer [<sub >evt]]
   [iron.closure-utils :refer [debug?]]
   [trilystro.db :as db]
   [trilystro.events :as events]
   [trilib.firebase :as fb]
   [trilib.fsm :as fsm]
   [trilib.modal :as modal]
   [trilystro.temp-utils :as tmp-utils]))


(defn mini-button
  "Icon-only button, good for standard actions"
  [icon options]
  [na/button (into {:icon icon
                    :floated "right"
                    :color "brown"
                    :size "mini"}
                   options)])


(defn lystro-search-grid
  "Grid to show Lystro elements.
  [TODO] Might be cleaner to merge this into search-panel, below"
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
  (let [getter ::fsm/page-param-val
        setter ::fsm/update-page-param-val]
    [lystro-search-grid {:color "brown"}
     [na/container {}
      [na/dropdown {:inline? true
                    :value (<sub [::fb/user-settings [:tags-mode] :any-of])
                    :on-change (na/value->event-fn [::fb/commit-user-setting :tags-mode] {:default :any-of})
                    :options (na/dropdown-list [[:all-of "All of"] [:any-of "Any of"]] first second)}]
      [nax/tag-selector {:all-tags-sub            [::fb/all-tags]
                         :selected-tags-sub       [getter :tags]
                         :set-selected-tags-event [setter :tags]}]]
     [na/input {:type "url"
                :placeholder "Website..."
                :value     (<sub      [getter :url] "")
                :on-change (na/value->event-fn [setter :url])}]
     (let [corner (fn [icon side field]
                    (let [value (<sub [::fb/user-settings [field]])]
                      [na/label {:icon icon
                                 :corner side
                                 :size "mini"
                                 :class-name "clickable"
                                 :color (if value "orange" "brown")
                                 :on-click #(>evt [::fb/commit-user-setting field (not value)])}]))]
       [:div
        (corner "tags" "left" :tags-as-text?)
        (corner "linkify" "right" :url-as-text?)
        [nax/native-text-area
         {:rows 3
          :placeholder "Description..."
          :value      (<sub [getter :text] "")
          :on-change #(>evt [setter :text (-> % .-target .-value)])}]])]))


(defn lystro-results-panel
  "Render one Lystro"
  [{:keys [tags text url owner shared?] :as lystro}]
  (let [mine? (= owner (<sub [::fb/uid]))
        lystro (assoc lystro :original-shared? shared?)]
    [na/segment {:secondary? (not mine?)
                 :tertiary? (not shared?)
                 :class-name "lystro-result"}
     (when mine? (mini-button "delete"
                              {:on-click #(>evt (modal/goto :modal-confirm-delete lystro))}))
     (when mine? (mini-button "write"
                              {:on-click #(>evt (modal/goto :modal-edit-lystro lystro))}))
     [nax/draw-tags {:selected-tags-sub       [::fsm/page-param-val :tags]
                     :set-selected-tags-event [::fsm/update-page-param-val :tags]
                     :class-of-tag-sub        [::fb/tag-class-by-frequency]}
      tags]
     [:div {:on-click #(when mine? (>evt (modal/goto :modal-edit-lystro lystro)))
            :class-name (str "text break-long-words "
                             (if mine? "editable-text" "frozen-text"))}
      text]
     [:div {:class-name "url"}
      (tmp-utils/link-to url)]
     (when (not mine?)
       [:div {:class "owner-sig"}
        (<sub [::fb/user-pretty-name owner])])]))


;;; [TODO] Maybe move to utils, if this proves itself
(defn sort-by-alpha
  "Sort strings, ignoring case and non-alphabetic chars"
  [keyfn coll]
  (sort-by (comp #(apply str (re-seq #"[A-Z]" %))
                 (fnil str/upper-case "")
                 keyfn)
           coll))

(defn main-panel
  "The main screen"
  []
  [na/form {}
   [na/divider {:horizontal? true :section? true} "Search Lystros"]
   [search-panel]
   (let [selected-lystros
         (<sub [::fb/lystros {:tags-mode     (keyword (<sub [::fb/user-settings [:tags-mode] :any-of]))
                              :tags          (<sub [::fsm/page-param-val :tags])
                              :url           (<sub [::fsm/page-param-val :url])
                              :text          (<sub [::fsm/page-param-val :text])
                              :tags-as-text? (<sub [::fb/user-settings [:tags-as-text?]])
                              :url-as-text?  (<sub [::fb/user-settings [:url-as-text?]])}])]
     [:div
      [na/divider {:horizontal? true :section? true}
       (str "Results (" (count selected-lystros) ")")]
      `[:div {}
        ~@(mapv lystro-results-panel (sort-by-alpha :text selected-lystros))]
      [na/divider {:horizontal? true :section? true}]
      [na/container {}
       [na/button {:size "mini"
                   :icon "external share"
                   :content "export all"
                   :on-click #(>evt (modal/goto :modal-show-exports (<sub [::fb/lystros])))}]
       [na/button {:size "mini"
                   :icon "share"
                   :content "export current"
                   :on-click #(>evt (modal/goto :modal-show-exports selected-lystros))}]]])])


(defn login-logout-control []
  (let [user (<sub [::fb/user])]
    [na/menu-menu {:position "right"}
     [na/menu-item {:on-click #(>evt [(if user :sign-out :sign-in)])}
      (if user
        [na/label {:image true :circular? true}
         [na/image {:src (:photo-url user)}]
         (<sub [::fb/user-name])]
        "login...")]
     (let [connected? (:firebase/connected? (<sub [:firebase/connection-state]))]
       [na/menu-item {:icon (if connected? "signal" "wait")
                      :content (if connected? "online" "offline")}])] ))

(defn top-bar []
  [na/menu {:fixed "top"}
   [na/menu-item {:header? true
                  :on-click #(>evt (modal/goto :modal-about))}
    [na/icon {:name "tasks" :size "big"}]
    (<sub [::db/name])]
   [na/menu-item {:name "Add"
                  :disabled? (not (<sub [::fsm/in-page? :logged-in]))
                  :on-click #(>evt (modal/goto :modal-new-lystro))}]
   [na/menu-item {:name "About"
                  :disabled? (not (or (<sub [::fsm/in-page? :logged-in])
                                      (<sub [::fsm/in-page? :logged-out])))
                  :on-click #(>evt (modal/goto :modal-about))}]
   [login-logout-control]])



;;; We want to keep the Firebase ":on" subscriptions active, so need to mount them in the
;;; main panel. But, we don't want anything to show. We could use a display:none div, but
;;; this head-fake is more elegant, and seems to work works.
;;; [TODO] ^:export is probably not needed, but I've not tested removing it. See
;;;        discussion in Slack #clojurescript channel Sept 6-7 2017.
(defn ^:export null-op [x] "")

(defn app-view []
  [na/container {}
   (into [:div] (<sub [::modal/all-modal-views]))
   [top-bar]
   [na/container {:style {:margin-top "5em"}}
    [nax/google-ad
     :unit "half banner"
     :ad-client "ca-pub-7080962590442738"
     :ad-slot "5313065038"
     :test (when debug? "... ADVERT  HERE ...")]
    (when (<sub [::fsm/in-page? :logged-in])
      (let [open-state ;; Subs that should be held open for efficiency
            [(<sub [:firebase/on-value {:path (fb/private-fb-path [:lystros])}])
             (<sub [:firebase/on-value {:path (fb/private-fb-path [:user-settings])}])
             (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:lystros])}])
             (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:users-details])}])
             ]]
        (null-op open-state)
        [main-panel]))]])

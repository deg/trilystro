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
   [trilystro.config :as config]
   [trilystro.events :as events]
   [trilystro.firebase :as fb]
   [trilystro.fsm :as fsm]))

;;; [TODO] Move to utils
(defn compare-ci [s1 s2]
  (compare (str/upper-case s1) (str/upper-case s2)))

;;; [TODO] Move to utils
(defn sort-ci [l]
  (sort-by str/upper-case l))


(defn draw-tag [form tag]
  (let [selected-tags (<sub [:form-state form [:tags]] #{})
        selected? (contains? selected-tags tag)]
    [na/list-item {:key tag
                   :on-click #(>evt [:form-state form [:tags]
                                     ((if selected? disj conj) selected-tags tag)])}
     [:span {:class (str "tag "
                         (if selected? "selected-tag" "unselected-tag"))}
      tag]]))

(defn draw-tags [form tags]
  [na/list-na {:class-name "tags"
               :horizontal? true}
   (doall (map #(draw-tag form %)
               (sort-ci tags)))])


(defn keyword-adder [form]
  (let [old-tags (set (<sub [:all-tags]))
        new-tags (<sub [:form-state form [:tags]] #{})
        available-tags (sort-ci (clojure.set/difference old-tags new-tags))]
    [na/grid {:container? true}
     [na/grid-row {}
      [draw-tags form new-tags]]
     [na/grid-row {}
      `[:datalist {:id "tags"}
        ~(map (fn [tag] [:option {:key tag :value tag}])
              available-tags)]
      [sa/Input {:type :text
                 :list "tags"
                 :action {:icon "add"
                          :on-click (na/>event [:add-new-tag form])}
                 :placeholder "add tag"
                 :value     (<sub      [:form-state form [:new-tag]] "")
                 :on-change (na/>event [:form-state form [:new-tag]])}]]]))

(defn keyword-selector [form]
  (let [available-tags (sort-ci (<sub [:all-tags]))
        chosen-tags (sort-ci (<sub [:form-state form [:tags]] #{}))]
    [na/dropdown {:multiple? true
                  :button? true
                  :value chosen-tags
                  :on-change (na/>event [:form-state form [:tags]] #{} set)
                  :options (na/dropdown-list available-tags identity identity)}]))


(defn entry-panel []
  (let [pub? (<sub [:form-state :entry [:public?]])
        public? (if-not (nil? pub?)
                  pub?
                  (or (get-in (<sub [:user-settings]) [:default-public?])
                      false))]
    [na/form {:widths "equal"}
     [nax/labelled-field
      :label "Tags:"
      :inline? true
      :content [keyword-adder :entry]]
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
     [nax/labelled-field
      :label "Visibility:"
      :content [sa/Checkbox {:label "Public"
                             :default-checked public?
                             :on-change (na/>event [:form-state :entry [:public?]] false)}]]
     (let [connected? (:firebase/connected? (<sub [:firebase/connection-state]))]
       [na/form-button {:disabled? (not connected?)
                        :on-click (na/>event [:page :quit-modal [:commit-lystro
                                                                 (assoc (<sub [:form-state :entry])
                                                                        :public? public?)
                                                                 :entry]])
                        :icon (if connected? "add" "wait")
                        :content (if connected?
                                   (str "Save " (if public? "public" "private"))
                                   "(offline)")
                        :positive? true}])]))


(defn modal-entry-panel []
  (let [new?  (<sub [:in-page :modal-new-lystro])
        edit? (<sub [:in-page :modal-edit-lystro])]
    [na/modal {:open? (or new? edit?)
               :dimmer "blurring"
               :close-icon true
               :on-close (na/>event [:page :quit-modal [:form-state :entry nil nil]])}
     [na/modal-header {} (cond new? "Add Lystro"
                               edit? "Edit Lystro"
                               :default "???")]
     [na/modal-content {} [entry-panel]]]))


;;; [TODO] Move to sodium.utils once this matures a bit
;;; [TODO] The URL is tainted text. Is there any risk here?
(defn link-to
  "Create an HTTP link. Use some smarts re user intention"
  [url-string]
  (when url-string
    (let [url (if (str/includes? url-string "/")
                url-string
                (str "http://" url-string))]
      [:a {:href url} url-string])))


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
       [na/grid-column value-params text]]]))

(defn delete-button [handler]
  [na/button {:icon "delete"
              :floated "right"
              :color "brown"
              :size "mini"
              :on-click handler}])

(defn lystro-results-panel [search-form {:keys [tags text url owner public?] :as lystro}]
  (let [mine? (= owner (<sub [:uid]))]
    [na/segment {:secondary? (not mine?)
                 :tertiary? (not public?)
                 :class-name "lystro-result"}
     (when mine?
       (delete-button (na/>event [:clear-lystro lystro])))
     (draw-tags search-form tags)
     [:div {:on-click #(when mine?
                         (>evt [:page :modal-edit-lystro [:form-state :entry nil lystro]]))
            :class-name (str "text "
                             (if mine? "editable-text" "frozen-text"))}
      text]
     [:div {:class-name "url"} (link-to url)]
     (when (not mine?)
       (let [user-details (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:user-details])}])
             user ((keyword owner) user-details)]
         [:div {:class "owner-sig"}
          (or (:display-name user) (:email user))]))]))


(defn main-panel []
  (fn []
    (let [selected-tags (set (<sub [:form-state :search [:tags]]))
          selected-url (<sub [:form-state :search [:url]])
          selected-text (<sub [:form-state :search [:text]])
          tags-mode (<sub [:form-state :search [:tags-mode]])
          lystros (<sub [:lystros {:tags-mode tags-mode :tags selected-tags :url selected-url :text selected-text}])]
      [na/form {:widths "equal"}
       [na/divider {:horizontal? true :section? true} "Search Lystros"]
       [lystro-grid {:color "brown"}
        [na/container {}
         [na/dropdown {:inline? true
                       :value     (<sub      [:form-state :search [:tags-mode]] :any-of)
                       :on-change (na/>event [:form-state :search [:tags-mode]] :any-of keyword)
                       :options (na/dropdown-list [[:all-of "All of"] [:any-of "Any of"]] first second)}]
         [keyword-selector :search]]
        [na/input {:type "url"
                   :placeholder "Website..."
                   :value     (<sub      [:form-state :search [:url]] "")
                   :on-change (na/>event [:form-state :search [:url]])}]
        [na/text-area {:rows 3
                       :placeholder "Description..."
                       :value     (<sub      [:form-state :search [:text]] "")
                       :on-change (na/>event [:form-state :search [:text]])}]]
       [na/divider {:horizontal? true :section? true} (str "Results (" (count lystros) ")")]
       `[:div {}
         ~@(mapv (partial lystro-results-panel :search) lystros)]])))


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
             :on-close (na/>event [:page :quit-modal])}
   [na/modal-header {}
    (str "About " (<sub [:name]))]
   [na/modal-content {}
    [about-panel]]])


(defn login-logout-control []
  (let [user (<sub [:user])]
    [na/menu-menu {:position "right"}
     (let [connected? (:firebase/connected? (<sub [:firebase/connection-state]))]
       [na/menu-item {:icon (if connected? "signal" "wait")
                      :content (if connected? "online" "offline")}])
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
    [na/icon {:name "eye" :size "big"}]
    (<sub [:name])]
   [na/menu-item {:name "Add"
                  :disabled? (not (<sub [:in-page :logged-in]))
                  :on-click (na/>event [:page :modal-new-lystro [:form-state :entry nil nil]])}]
   [na/menu-item {:name "About"
                  :on-click (na/>event [:page :modal-about])}]
   [login-logout-control]])


;; [TODO] Move this to Sodium extensions
(defn google-ad [& {:keys [unit ad-client ad-slot test]}]
  (reagent/create-class
   {:display-name "google-ad"
    :component-did-mount
    #(when (and js.window.adsbygoogle (not test))
       (. js.window.adsbygoogle push {}))
    :reagent-render
    (fn [& {:keys [unit ad-client ad-slot]}]
      [na/advertisement {:unit unit :centered? true :test test}
       (when-not test
         [:ins {:class-name "adsbygoogle"
                :style {:display "block"}
                :data-ad-format "auto"
                :data-ad-client ad-client
                :data-ad-slot ad-slot}])])}))



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
     (let [open-state [(<sub [:firebase/on-value {:path (fb/public-fb-path [:tags])}])
                       (<sub [:firebase/on-value {:path (fb/private-fb-path [:lystros])}])
                       (<sub [:firebase/on-value {:path (fb/private-fb-path [:user-settings])}])
                       (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:lystros])}])
                       (<sub [:firebase/on-value {:path (fb/all-shared-fb-path [:user-details])}])]]
       [na/container {:style {:margin-top "5em"}}
        (null-op open-state)
        [google-ad
         :unit "half banner"
         :ad-client "ca-pub-7080962590442738"
         :ad-slot "5313065038"
         :test (when config/debug? "... ADVERT  HERE ...")]
        [modal-entry-panel]
        [main-panel]]))] )

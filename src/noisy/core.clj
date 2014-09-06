(ns noisy.core
  (:import [java.awt.image BufferedImage]
           [java.awt Color]
           [java.util Random]
           [javax.imageio ImageIO]
           [java.io File]
           [com.google.common.hash Hashing])
  (:require [clojure.math.numeric-tower :as math]))

(defn- normalize-int [i]
  (cond
   (pos? i) (double (/ i Integer/MAX_VALUE))
   (neg? i) (- (double (/ i Integer/MIN_VALUE)))
   :else i))

; TODO: here's an idea: use caching/memoization as a 'middleware'/proxy
;       to the rnd-gen. We only need to cache 'width' many entries when
;       traversing x/y... 'height' in the y/x case.
;       -> follow-up idea traverse to minimize storage requirement

;;; implicit functions
(defn murmur-random-generator
  "Returns a fn that given ints as input returns a deterministic PRN
   Double in [-1.0, 1.0]."
  ([]
     (murmur-random-generator 0))
  ([seed]
     (let [hashing (Hashing/murmur3_32 seed)]
       (fn [& vals]
         (let [hasher (.newHasher hashing)]
           (doseq [val vals]
             (.putInt hasher val))
           (-> (.asInt (.hash hasher))
               normalize-int))))))

;;; generators
(defn sample-coordinates [lower]
  (reduce (fn [acc v]
            (let [upper (inc v)]
              (if (seq acc)
                (for [new [v upper]
                      old acc]
                  (conj old new))
                [[v] [upper]])))
          nil
          lower))

(defn fade1 [t]
  (* t t (- 3 (* 2 t))))

(defn fade2 [t]
  (* t t t (+ 10
              (* -15 t)
              (* 6 t t))))

(defn linear-interpolation
  ([t]
     (linear-interpolation t 0 1))
  ([t a b]
     (+ a (* t (- b a)))))

(defn perlin [generator curve-fn]
  (fn [& coords]
    (let [lower (map (comp int math/floor) coords)
          fractionals (map - coords lower)
          curves (map curve-fn fractionals)
          samples (sample-coordinates lower)
          ; the samples are ordered like so:
          ; (c/sample-coordinates [0 0 0])
          ; ([0 0 0] [1 0 0] [0 1 0] [1 1 0] [0 0 1] [1 0 1] [0 1 1] [1 1 1])
          ; therefore we can partion into 2 for each axis during
          ; the interpolation steps... pretty clever
          gradients (map (partial apply generator) samples)
          ]
      (loop [[curve & curves'] curves
             grads gradients]
        (let [groups (partition 2 grads)
              grads' (map (partial apply linear-interpolation curve)
                          groups)]
          (if (seq curves')
            (recur curves'
                   grads')
            (first grads')))))))

;;; modifiers
(defn scale [source & factors]
  (fn [& coords]
    {:pre [(or (= 1 (count factors))
               (= (count factors)
                  (count coords)))]}
    (apply source
           (map #(/ % %2)
                coords
                (if (= 1 (count factors))
                  (repeat (first factors))
                  factors)))))

(defn floor [source]
  (fn [& coords]
    (apply source (map #(int (math/floor %)) coords))))

;;; RGBA functions
(defn- gray
  "Returns a gray-scale rgb representation given [-1.0, 1.0] as input"
  [i]
  (let [j (int (* 128 (inc i)))]
    (.getRGB (Color. j j j))))

(defn paint [generator width height & opts]
  {:pre [width height generator]}
  (let [file-name (or (first opts) "image.png")
        image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)]
    (doseq [x (range width)
            y (range height)]
      (.setRGB image x y (gray (generator x y))))
    (ImageIO/write image "png" (File. file-name))))

#_
(
 (require '[noisy.core :as c])
 (c/paint (-> (c/murmur-random-generator)
              (c/floor)
              (c/scale 10))
          200 200)

 (c/paint (-> (c/perlin (c/murmur-random-generator) c/fade1)
              (c/scale 10))
          200 200)
 )

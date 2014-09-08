# noisy

TODO

## Usage

### Paint Noise

``` clojure
(require '[noisy.core :refer :all])
(paint2d (murmur-noise) 200 200)
```

![](doc/noise2d.png)

``` clojure
(require '[noisy.core :as c])
(paint1d (murmur-noise) 400 100)
```

![](doc/noise1d.png)

### Scale Noise


```clojure
(paint2d (-> (murmur-noise)
             (floor)
             (scale 10)) 200 200)
```

... Minecraft anyone?

![](doc/scaled_noise.png)

```clojure
(paint1d (-> (murmur-noise)
             (floor)
             (scale 10)) 400 100)
```

![](doc/scaled_1dnoise.png)

### Perlin Noise

#### Generic example

My perlin implementation that uses a murmur based gradient-generator
which is currently much slower than the classical perlin implementation:

```clojure
(paint2d (-> (perlin)
             (scale 10)) 200 200)
```

![Perlin Noise](doc/perlin_noise.png)

For the classical, improved perlin noise:

(paint2d (-> (perlin)
             (scale 10)) 200 200)
```

![Perlin Noise](doc/perlin_noise_improved.png)

```clojure
(paint1d (-> (perlin-improved)
             (scale 40)) 400 100)
```

![](doc/perlin_noise_1d.png)

#### Close-up

```clojure
(paint2d (-> (perlin)
             (scale 50)) 200 200)
```

![Close-up](doc/perlin_closeup.png)

#### Linear Interpolation (Yikes!)

```clojure
(paint2d (-> (perlin :curve-fn linear-interpolation)
             (scale 10)) 200 200)
```

![Perlin Noise using linear interpolation... Yikes!](doc/perlin_linear.png)

## Utilities

### Grid & File export

```clojure
(paint2d (-> (perlin)
             (scale 40)) 200 200 :grid 40 :file "grid2d.png")
```

![](doc/grid2d.png)

```clojure
(paint1d (-> (perlin)
             (scale 40)) 400 100 :grid 40 :file "grid1d.png")
```

![](doc/grid1d.png)

## TODOs
 - Add caching middleware
 - Add tiling
 - Add offset
 - Add paint3d:
   - export depth many images
   - export to animated GIF(?)

## License

TODO

Copyright © 2014 Roman Flammer

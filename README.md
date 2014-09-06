# noisy

TODO

## Usage

### Paint Noise

``` clojure
(require '[noisy.core :as c])
(c/paint (c/murmur-random-generator) 200 200)
```

![](doc/noise.png)

### Scale Noise


```clojure
(c/paint (-> (c/murmur-random-generator)
             (c/floor)
             (c/scale )) 200 200)
```

... Minecraft anyone?

![](doc/scaled_noise.png)

### Perlin Noise

#### Generic example

```clojure
(c/paint (-> (c/perlin (c/murmur-random-generator) c/fade1)
             (c/scale 10)) 200 200)
```

![Perlin Noise](doc/perlin_noise.png)

#### Close-up

```clojure
(c/paint (-> (c/perlin (c/murmur-random-generator) c/fade1)
             (c/scale 100)) 200 200)
```

![Close-up](doc/perlin_closeup.png)

#### Linear Interpolation (Yikes!)

```clojure
(c/paint (-> (c/perlin (c/murmur-random-generator) c/linear-interpolation)
             (c/scale 100)) 200 200)
```

![Perlin Noise using linear interpolation... Yikes!](doc/perlin_linear.png)

## License

TODO

Copyright © 2014 Roman Flammer

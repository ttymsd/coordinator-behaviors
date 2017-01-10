Coordinator Behaviors
=====================
![License](https://img.shields.io/hexpm/l/plug.svg)
[ ![Download](https://api.bintray.com/packages/ttymsd/maven/jp.bglb.bonboru%3Acoordinator-behaviors/images/download.svg) ](https://bintray.com/ttymsd/maven/jp.bglb.bonboru%3Acoordinator-behaviors/_latestVersion)

## Demo

<img width="50%" src="art/google_map_behavior.gif">
<img width="50%" src="art/youtube_behavior.gif">

## Description
### Usage

Add dependency to `build.gradle`.

```gradle
repositories {
    maven {
        url  "http://dl.bintray.com/ttymsd/maven" 
    }
}
dependencies {
    compile "jp.bglb.bonboru:coordinator-behaviors:$latestVersion"
}
```

### GoogleMapLikeBehavior

View slide up from bottom as GoogleMapApp.

This Behavior sample is [here](https://github.com/ttymsd/coordinator-behaviors/blob/master/example/src/main/kotlin/jp/bglb/bonboru/behaviors/app/GoogleMapBehaviorActivity.kt)

This code base on [CustomBottomSheetBehavior](https://github.com/miguelhincapie/CustomBottomSheetBehavior) rewritten by kotlin and add some features.

Diff from CustomBottomSheetBehavior on 2017/01/09

- Don't need Toolbar background as Other View
- Add some attributes for behaviors
- Available to skip anchor point.
- Write by kotlin

### YoutubeLikeBehavior

This Behavior that makes it possible to drag View like the Youtube app.

This Behavior sample is [here](coordinator-behaviors/example/src/main/kotlin/jp/bglb/bonboru/behaviors/app/YoutubeBehaviorActivity.kt)

### Todo

- BottomNavigationBehavior
- FABBehavior

### License

```text
Copyright 2017 Tetsuya Masuda

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
## About
Image Finder is android application which uses OCR(Optical Character Recognition) and Image Classification to generate search text queries which are then used by the user to find the image. Once found user can choose to share the image on the social media applications. Image finder can help find image on local directory just by using the text in them or the objects in them (say cat/dog). The application is intended to be minimal and fast.

## Details
The text found by OCR is converted to text query and saved to a .txt file in app/data/data, which is later matched with user's search query and displayed in Grid Search when found, same is with Image Classification. Only at first run the models scan for texts/classes, in later runs application checks for file changes, if found then only Rescan the deorectory, else use the saved .txt file to match queries to save computations. It uses [ml-kit](https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart) for OCR and Image Classification. Image classification model is Image-Net pretrained with tensorflow hosted [here](https://www.tensorflow.org/lite/guide/hosted_models). Currently limited directories are added to find images(DCIM, Download, PICTURES, Bluetooth, Edited), one can easily add them in "get_all_images()" function under MainActivity.java.

## Screens
![1](/Screens/s1.png) 
![2](/Screens/s2.png) 
![3](/Screens/s3.png) 

## Features
* Fast and easy to use UI.
* Finds images and share with one tap.
* Uses newest ML-Kit-Vision API.
* Clean code and many of comments.
* Android 7.0 - 10.0 support.
* Easy to transport code to your gallery application.
* Find .apk file [here](https://drive.google.com/file/d/1NX1PZtpgUx8YRT83dvjX4H4ceqqq-0fv/view?usp=sharing).

## Applications
* Find meme in local directory.
* Find your documents from directory.

## Improvements
* Currently only reads of images in cureent dirs and not sub-dirs to save computation time.
* Use MRPC to match search queries.
* Find Faces too, first tag them, make a query out of it, then match faces with tagged face.
* Maybe not supported on Android 11(due to storage permission scope).
* Not Working on Emulators, due to the play store services version not up-to-date(the app requires latest version).

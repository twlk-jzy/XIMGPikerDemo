# XIMGPikerDemo
一款多图片选择器



首先先看下效果吧，因为是android 原生模拟器录制的gif，android8.0系统录制的  效果不是太好，后续更新吧。。。。。

![image](http://upload-images.jianshu.io/upload_images/2440575-0b6268219f323741.gif?imageMogr2/auto-orient/strip)

# 使用 

1、在你需要使用的地方  直接使用Intent跳转到 PickOrTakeImageActivity   如：
Intent intent =newIntent(MainActivity.this,PickOrTakeImageActivity.class);

startActivityForResult(intent,0x001);


2、重写onActivityResult  方法，然后使用
ArrayList imgs = data.getStringArrayListExtra("data");
获取得到一个图片集合，得到的是  本地图片的路径，然后就可以使用这些路径去操作图片啦



最后，此项目已经上传到jcenter仓库了，放下引入方式吧。。。。

compile'com.twlk:XIMGPiker:1.0.2'


# 说明
此项目中引入的第三方依赖库有,感谢

compile'com.jakewharton:butterknife:8.7.0'

compile'com.github.bumptech.glide:glide:3.8.0'

compile'com.android.support:design:25.2.0'

compile'top.zibin:Luban:1.1.2'



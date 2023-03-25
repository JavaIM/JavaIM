# JavaIM示例插件项目
## 开发者食用方法
将JavaIM最新Jar文件改名为JavaIM.jar放到示例插件项目下的dep文件夹(重要！无此操作，执行时将会报错）

然后根据正常java开发进行开发
### 如果您要重构项目名称，请进行以下修改
打开pom.xml 找到

    <groupId>org.example</groupId>
    <artifactId>JavaIMExamplePlugin</artifactId>
    <version>1.0-SNAPSHOT</version>

将groupId设置为PackageID，artifactId设置为工件id

打开src/main/resources/PluginManifest.properties

找到Main-Class=org.example.JavaIMExamplePlugin.PluginMain

将他修改为Main-Class=PackageID.工件id.主class名

全程不能有英文

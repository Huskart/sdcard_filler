# sdcard_filler
手机sdcard、system分区填充

<img src="http://i.imgur.com/RYlWZfI.png" width=220 height=380/>

项目很简单，一个界面外加几个工具类。
项目中用到一个自定义View，看起来比较复杂，其实仔细读后就会发现，其实都是满满的套路，
自定义View主要分三个部分，外层紫色的刻度，内层白色的刻度，两个刻度中间的一段弧线，指针，还有数字。刻度的实现是通过虚线，计算虚线的间隔实现的。
通过DashPathEffect()实现虚线效果,参数分别是线的粗细和间隔的大小
```
  paint.setPathEffect(new DashPathEffect(new float[] { lineWidth, lineSpace }, 0));
```

其中比较关键的地方就是设置仪表盘的两个值。需要在VelocimeterView类的初始化方法initAttributes()中做些修改
```
if (units == null) {
	File path = Environment.getExternalStorageDirectory();
	StatFs stat = new StatFs(path.getPath());
	long blockSize = stat.getBlockSize();
	long totalBlocks = stat.getBlockCount();
	int total = (int) (blockSize * totalBlocks / (1024 * 1024));
	units = "   /  " + (total) + " (Mb)";
}
```

在Activity初始化的时候设置值

```
this.mVelocimeterView = (VelocimeterView) findViewById(R.id.velocimeter2);
this.mVelocimeterView.setMax(100);
float totalSpc = Float.parseFloat(getSDTotalSize());
float remainSpc = Float.parseFloat(getSDAvailableSize());
float value = ((totalSpc - remainSpc) / totalSpc) * 100;
this.mVelocimeterView.setValue(value, true);

```

另外练手，项目中使用了RxJava，另外作者保留了一个Thread+Handler更新UI的模式，大家可以用来进行对比。

关于填充，主要原理是通过createFixLengthFile()方法创建一个固定大小的文件,FileChannel的使用大家可先自行百度。
```
public void createFixLengthFile(File file, long length) throws IOException {
        long start = System.currentTimeMillis();
        FileOutputStream fos = null;
        FileChannel output = null;
        try {
            fos = new FileOutputStream(file);
            output = fos.getChannel();
            output.write(ByteBuffer.allocate(1), length - 1);
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("total times " + (end - start));
    }
```

由于FileChannel只是指定了文件的其实位置和终止的位置，中间并没有填充字节，所以我们需要在手动cp 复制一下刚才创建的那个文件，在复制过程中，文件会被真正填充内容，从而达到了填充存储空间的目的。

由于练手，就没有处理那种可插拔的扩展卡的情况

系统分区/system，原理和普通分区一样的，只不过多了个权限问题而已。

释放就很简单了，删掉就可以了。

欢迎交流。

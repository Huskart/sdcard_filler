package com.zexuan.filler.velocimeter.painter.needle;


import com.zexuan.filler.velocimeter.painter.Painter;

/**
 * @author Adrián García Lomas
 */
public interface NeedlePainter extends Painter {

  void setValue(float value);
}

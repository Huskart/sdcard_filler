package com.zexuan.filler.velocimeter.painter.digital;


import com.zexuan.filler.velocimeter.painter.Painter;

/**
 * @author Adrián García Lomas
 */
public interface Digital extends Painter {

  void setValue(float value, int type);

  void setUnit(String unit);
}

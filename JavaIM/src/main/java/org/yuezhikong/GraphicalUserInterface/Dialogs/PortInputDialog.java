package org.yuezhikong.GraphicalUserInterface.Dialogs;

import javafx.beans.NamedArg;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;

import java.lang.reflect.Field;

public class PortInputDialog extends TextInputDialog {
    /**
     * 端口输入对话框初始化
     * @throws NoSuchFieldException 无法找到TextInputDialog中的textField，初始化失败
     * @throws IllegalAccessException 无法访问textField，初始化失败
     */
    public PortInputDialog() throws NoSuchFieldException, IllegalAccessException {
        this("");
    }

    /**
     * 端口输入对话框初始化
     * @param DefaultTextFieldValue textField中默认的数值
     * @throws NoSuchFieldException 无法找到TextInputDialog中的textField，初始化失败
     * @throws IllegalAccessException 无法访问textField，初始化失败
     */
    public PortInputDialog(@NamedArg("defaultValue") String DefaultTextFieldValue) throws NoSuchFieldException, IllegalAccessException {
        super(DefaultTextFieldValue);
        Field field = TextInputDialog.class.getDeclaredField("textField");
        field.setAccessible(true);
        TextField textField = (TextField) field.get(this);
        field.setAccessible(false);
        textField.setTextFormatter(new TextFormatter<String>(change -> {
            try
            {
                if (!("".equals(change.getText())))
                {
                    int integer = Integer.parseInt(change.getText());
                    if (integer > 65535
                            || integer < 0
                            || (Integer.parseInt(textField.getText() + integer) > 65535)
                            || (Integer.parseInt(textField.getText() + integer) == 0))
                    {
                        return null;
                    }
                }
            } catch (NumberFormatException e)
            {
                return null;
            }
            return change;
        }));
    }
}

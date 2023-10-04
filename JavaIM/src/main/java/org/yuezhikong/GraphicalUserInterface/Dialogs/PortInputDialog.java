package org.yuezhikong.GraphicalUserInterface.Dialogs;

import javafx.beans.NamedArg;
import javafx.scene.control.*;

public class PortInputDialog extends TextInputDialog {
    /**
     * 端口输入对话框初始化
     */
    public PortInputDialog() {
        this("");
    }

    private boolean CheckInputData(String data,TextField textField)
    {
        try
        {
            if (!("".equals(data)))
            {
                int integer = Integer.parseInt(data);
                if (integer > 65535
                        || integer < 0
                        || (Integer.parseInt(textField.getText() + integer) > 65535)
                        || (Integer.parseInt(textField.getText() + integer) == 0))
                {
                    return false;
                }
            }
        } catch (NumberFormatException e)
        {
            return false;
        }
        return true;
    }
    /**
     * 端口输入对话框初始化
     * @param DefaultTextFieldValue textField中默认的数值
     */
    public PortInputDialog(@NamedArg("defaultValue") String DefaultTextFieldValue) {
        super(DefaultTextFieldValue);
        TextField textField = this.getEditor();
        textField.clear();
        textField.setTextFormatter(new TextFormatter<String>(change -> {
            if (CheckInputData(change.getText(),textField))
            {
                return change;
            }
            else
            {
                return null;
            }
        }));
        ((Button) (this.getDialogPane().lookupButton(ButtonType.OK))).setOnAction((actionEvent) -> textField.clear());
    }
}

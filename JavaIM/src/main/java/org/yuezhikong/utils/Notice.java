package org.yuezhikong.utils;

import java.io.File;
import java.util.Scanner;

public class Notice {
    public Notice() {
        if (!(new File("server.properties").exists())) {
            int Input;
            Scanner scanner;
            do {
                System.out.println("在继续使用JavaIM之前，请您仔细阅读软件使用的注意事项。");
                System.out.println("本软件JavaIM是一个遵循GNU通用公共许可证第三版（GPL-3）的开源软件。这意味着您可以自由地运行、复制、分发、研究、修改和改进这个软件。");
                System.out.println("使用GPL-3协议的软件，您应该遵循以下规定：");
                System.out.println("1.若您分发本软件或基于本软件的衍生作品，您必须以GPL-3协议进行开源，向接收者提供源代码。");
                System.out.println("2.若您修改本软件并分发，您必须以GPL-3协议进行开源，并明确标明您修改过的部分。");
                System.out.println("3.您不得对GPL-3协议的软件或基于GPL-3协议的软件添加任何形式的技术限制，使得接收者不能享受到GPL-3协议赋予的权利。");
                System.out.println("本软件是一种具有端对端通讯功能的加密通讯软件，我们强烈建议您在遵守当地法律法规的前提下使用。");
                System.out.println("我们提醒您，使用本软件可能涉及到数据安全和隐私保护等问题，您应自行评估风险。我们不对您使用本软件所造成的任何直接、间接、特殊、偶然的损失或伤害承担任何责任。");
                System.out.println("我们对本软件的正确性、安全性、连续性、及时性和性能不作任何明示或暗示的保证。您应自行承担使用本软件的风险。");
                System.out.println("使用本软件即表示您已阅读并清楚了解了以上注意事项，请输入1以继续");
                scanner = new Scanner(System.in);
                Input = scanner.nextInt();
            } while (Input != 1);
        }
    }
}

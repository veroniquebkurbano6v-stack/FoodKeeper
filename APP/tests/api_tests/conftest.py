"""
pytest 配置文件

配置测试发现路径、标记、报告输出等
"""

import sys
import os

# 将项目根目录和common目录添加到Python路径
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(__file__)), "common"))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(__file__)), "config"))
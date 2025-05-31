#
# Be sure to run `pod lib lint PDMUIImpl.podspec' to ensure this is a
# valid spec before submitting.
#
# Any lines starting with a # are optional, but their use is encouraged
# To learn more about a Podspec see https://guides.cocoapods.org/syntax/podspec.html
#

Pod::Spec.new do |s|
    s.name             = 'CMPUIKitUtils'
    s.version          = '0.0.3'
    s.summary          = '请简单的介绍一下你的组件 CMPUIKitUtils.'
    s.description      = <<-DESC
        TODO: 给你的组件添加一段描述文案。
    DESC

    s.homepage         = 'https://git.code.oa.com/krauschen/CMPUIKitUtils'
    s.license          = { :type => 'MIT', :file => 'LICENSE' }
    s.author           = { 'krauschen' => 'krauschen@tencent.com' }
    s.source           = { :git => 'http://git.code.oa.com/NextLib/CMPUIKitUtils', :tag => s.version.to_s }
    s.ios.deployment_target = '11.0'
    s.library = 'c++'

    s.xcconfig = {
         'CLANG_CXX_LANGUAGE_STANDARD' => 'gnu++17',
         'CLANG_CXX_LIBRARY' => 'libc++',
    }

    ###### 文件引用 ######

    # 不建议直接这样引用资源文件，但考虑老组件的迁移成本，默认使用这种方式。
    s.source_files = 'CMPUIKitUtils/**/**/*'

    # 目前强制使用静态库引入
    s.static_framework = true
end

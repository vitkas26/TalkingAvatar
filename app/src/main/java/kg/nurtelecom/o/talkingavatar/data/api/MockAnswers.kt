package kg.nurtelecom.o.talkingavatar.data.api

// Заглушка backend: канонические ответы на каждом из поддерживаемых языков (Language.code).
val mockAnswersByLanguage: Map<String, List<String>> = mapOf(
    "ru-RU" to listOf(
        "В магазинах О!Сторе сейчас отличные условия на покупку смартфонов. Вы можете взять любой гаджет в рассрочку без процентов и первого взноса\n" +
            "При покупке смартфонов Samsung или Xiaomi мы дарим до 100 гигабайт интернета в подарок\n" +
            "Заходите в наши филиалы, консультанты помогут подобрать аксессуары и настроить ваш новый девайс",

        "Вы уже пользуетесь кошельком О!Деньги в приложении Мой О!? Это очень удобно\n" +
            "Вы можете оплачивать более тысячи услуг, включая коммунальные платежи, штрафы и налоги, прямо с баланса телефона\n" +
            "За каждую оплату по QR-коду мы начисляем кешбэк, который можно тратить на оплату связи или покупки в магазинах-партнерах",

        "Забудьте о пластиковых сим-картах! В сети О! вы можете подключить eSIM за считанные минуты\n" +
            "Это цифровая карта, которую невозможно потерять или повредить. Она позволяет использовать два номера даже на тех айфонах, где всего один слот для симки\n" +
            "Подключить её можно удаленно в приложении или в любом офисе обслуживания",

        "Путешествуйте с О! по всему миру. Наши выгодные пакеты роуминга работают более чем в 100 странах\n" +
            "Кстати, у нас появилась уникальная услуга — интернет на борту самолета! Теперь вы можете оставаться в сети даже на высоте 10 тысяч метров\n" +
            "Просто подключите роуминг перед вылетом, и ваши мессенджеры будут активны весь полет",

        "Превратите свой смартфон в настоящий кинотеатр с услугой О!ТиВи\n" +
            "Вам доступны более 200 каналов в высоком качестве и сразу 5 топовых онлайн-кинотеатров, включая START и Wink\n" +
            "Весь интернет-трафик при просмотре видео внутри приложения абсолютно бесплатен и не расходует ваш основной пакет",

        "Мы внедрили технологию VoLTE для всех наших абонентов! Теперь голос при звонках звучит максимально четко, как будто собеседник стоит рядом\n" +
            "Главное преимущество — интернет не прерывается во время разговора. Вы можете продолжать скачивать файлы или пользоваться навигатором прямо во время звонка\n" +
            "Услуга предоставляется совершенно бесплатно",

        "Установите приложение Мой О!, чтобы полностью контролировать свой номер\n" +
            "Там можно мгновенно сменить тариф, проверить остаток гигабайт, детализировать расходы и даже вызвать такси\n" +
            "А еще в приложении часто проходят игры с крутыми призами — от бесплатных пакетов связи до современных смартфонов",

        "С заботой о вашей безопасности мы разработали сервис Где дети. Он позволяет видеть местоположение ваших близких на карте в реальном времени\n" +
            "Вы будете получать уведомления, когда ваш ребенок пришел в школу или вернулся домой\n" +
            "Услуга работает даже если на телефоне ребенка не включен интернет, что очень надежно в экстренных ситуациях",

        "Хотите подчеркнуть свою индивидуальность? В О! можно выбрать эксклюзивный золотой номер\n" +
            "Легко запоминающиеся комбинации цифр отлично подходят для бизнеса или личного использования\n" +
            "Выбрать и забронировать красивый номер можно прямо на нашем сайте или в приложении в разделе Мой номер",

        "Сеть О! — это лидер по покрытию 4G в Кыргызстане. Мы постоянно строим новые базовые станции даже в самых отдаленных районах страны\n" +
            "Сейчас мы активно тестируем технологию 5G, чтобы в будущем наши абоненты могли пользоваться космическими скоростями интернета\n" +
            "С нами вы всегда будете онлайн — и в центре города, и высоко в горах",
    ),

    "ky-KG" to listOf(
        "О!Стор дүкөндөрүндө учурда смартфондорго сонун шарттар бар. Каалаган гаджетти пайызсыз жана алгачкы төлөмсүз бөлүп төлөп ала аласыз\n" +
            "Samsung же Xiaomi смартфонун сатып алганда биз сизге 100 гигабайтка чейин интернет белек кылабыз\n" +
            "Филиалдарыбызга келиңиз, консультанттарыбыз аксессуар тандап, жаңы түзмөгүңүздү жөндөп берет",

        "Мобилдик О! колдонмосундагы О!Акча капчыгын колдонуп жатасызбы? Бул абдан ыңгайлуу\n" +
            "Телефон балансыңыздан коммуналдык төлөмдөр, айыптар жана салыктарды кошкондо миңден ашык кызматка төлөй аласыз\n" +
            "QR-код аркылуу ар бир төлөм үчүн байланышка же өнөктөш дүкөндөрдө сарптай турган кэшбэк аласыз",

        "Пластик SIM карталарды унутуңуз! О! тармагында бир нече мүнөттүн ичинде eSIM туташтыра аласыз\n" +
            "Бул жоголбой турган жана бузулбай турган санарип карта. Бир слоту бар iPhone'до да эки номерди колдонууга мүмкүндүк берет\n" +
            "Аны колдонмо аркылуу алыстан же кызмат көрсөтүү офисинде туташтырсаңыз болот",

        "О! менен дүйнөнү кыдырыңыз. Пайдалуу роуминг пакеттерибиз 100дөн ашык өлкөдө иштейт\n" +
            "Айтмакчы, бизде уникалдуу кызмат пайда болду — учактагы интернет! Эми 10 миң метр бийиктикте да байланышта кала аласыз\n" +
            "Учуудан мурун роумингди күйгүзүңүз, мессенджерлериңиз бүт учуу бою активдүү болот",

        "О!ТиВи кызматы менен смартфонуңузду чыныгы кинотеатрга айландырыңыз\n" +
            "Сизге 200дөн ашык жогорку сапаттагы канал жана START, Wink кошкондо 5 мыкты онлайн-кинотеатр бир учурда жеткиликтүү\n" +
            "Колдонмонун ичинде видео көрүүдөгү бардык интернет трафиги толугу менен акысыз жана негизги пакетиңизди сарптабайт",

        "Биз бардык абоненттерибиз үчүн VoLTE технологиясын киргиздик! Эми чалуулардагы үн өтө так угулат, сүйлөшкөн адам жаныңызда тургандай\n" +
            "Эң башкы артыкчылыгы — сүйлөшүү учурунда интернет үзүлбөйт. Чалуу учурунда файл жүктөп же навигацияны колдонсоңуз болот\n" +
            "Кызмат толугу менен акысыз көрсөтүлөт",

        "Номериңизди толук көзөмөлдөө үчүн Мобилдик О! колдонмосун орнотуңуз\n" +
            "Анда тарифти дароо алмаштырсаңыз, калган гигабайтты текшерсеңиз, чыгымдарды майдалап көрсөңүз, ал тургай такси чакырсаңыз болот\n" +
            "Колдонмодо ошондой эле акысыз интернет пакеттеринен баштап заманбап смартфондорго чейин жуткан оюндар үзгүлтүксүз өтүп турат",

        "Коопсуздугуңуз үчүн биз \"Балдар кайда\" кызматын иштеп чыктык. Ал жакындарыңыздын жайгашкан жерин картада реалдуу убакытта көрүүгө мүмкүндүк берет\n" +
            "Балаңыз мектепке келгенде же үйгө кайтканда сизге билдирүү келет\n" +
            "Баланын телефонунда интернет күйгүзүлбөсө да кызмат иштейт, бул өзгөчө кырдаалдарда абдан ишенимдүү",

        "Өзгөчөлүгүңүздү көрсөткүңүз келеби? О!до сиз өзгөчө алтын номерди тандай аласыз\n" +
            "Оңой эсте каларлык сан айкалыштары бизнес же жеке колдонуу үчүн эң ылайыктуу\n" +
            "Сулуу номерди биздин сайтта же колдонмодогу \"Менин номерим\" бөлүмүндө тандап, брондоп коё аласыз",

        "О! тармагы Кыргызстанда 4G камтуу боюнча лидер. Биз өлкөнүн эң алыскы аймактарында да жаңы базалык станцияларды курууну улантып жатабыз\n" +
            "Азыр биз 5G технологиясын активдүү тестирлеп жатабыз, келечекте абоненттерибиз интернеттин космостук ылдамдыгынан пайдалана алышы үчүн\n" +
            "Биз менен сиз ар дайым онлайндасыз — шаардын борборунда да, тоонун бийигинде да",
    ),

    "en-US" to listOf(
        "O!Store shops currently have great deals on smartphones. You can get any device on interest-free installments with no down payment\n" +
            "When you buy a Samsung or Xiaomi smartphone, we give you up to 100 GB of free internet\n" +
            "Come by our branches — our consultants will help you pick accessories and set up your new device",

        "Are you already using the O!Money wallet in the My O! app? It's incredibly convenient\n" +
            "You can pay for over a thousand services, including utility bills, fines, and taxes, straight from your phone balance\n" +
            "Every QR-code payment earns you cashback, which you can spend on mobile services or purchases at partner stores",

        "Forget plastic SIM cards! With the O! network, you can activate an eSIM in just a few minutes\n" +
            "It's a digital card that can't be lost or damaged. It lets you use two numbers even on iPhones with only one SIM slot\n" +
            "You can activate it remotely in the app or at any service office",

        "Travel the world with O!. Our great roaming packages work in more than 100 countries\n" +
            "By the way, we now offer a unique service — in-flight internet! You can stay connected even at 10,000 meters\n" +
            "Just turn on roaming before your flight, and your messengers will stay active for the whole trip",

        "Turn your smartphone into a real movie theater with O!TV\n" +
            "You get access to more than 200 channels in high quality, plus 5 top streaming services at once, including START and Wink\n" +
            "All internet traffic for watching video inside the app is completely free and doesn't use your main data package",

        "We've rolled out VoLTE for all our subscribers! Now calls sound crystal clear, as if the other person were right next to you\n" +
            "The main benefit — your internet connection doesn't drop during calls. You can keep downloading files or using navigation while on a call\n" +
            "The service is completely free",

        "Install the My O! app to take full control of your number\n" +
            "You can instantly switch tariffs, check your remaining gigabytes, get a detailed breakdown of spending, and even order a taxi\n" +
            "The app also regularly runs games with great prizes — from free data packages to the latest smartphones",

        "For your peace of mind, we built the Where Are the Kids service. It lets you see your loved ones' location on a map in real time\n" +
            "You'll get notified when your child arrives at school or gets back home\n" +
            "The service works even if there's no internet on your child's phone, which makes it very reliable in emergencies",

        "Want to stand out? With O! you can choose an exclusive gold number\n" +
            "Easy-to-remember digit combinations are great for business or personal use\n" +
            "You can pick and reserve a beautiful number right on our website or in the app, under the My Number section",

        "The O! network is the leader in 4G coverage in Kyrgyzstan. We keep building new base stations even in the most remote parts of the country\n" +
            "We're actively testing 5G technology right now, so our subscribers can enjoy blazing-fast internet speeds in the future\n" +
            "With us, you're always online — whether downtown or high up in the mountains",
    ),

    "tr-TR" to listOf(
        "O!Store mağazalarında şu anda akıllı telefonlarda harika fırsatlar var. Herhangi bir cihazı faizsiz ve peşinatsız taksitle alabilirsiniz\n" +
            "Samsung veya Xiaomi akıllı telefon aldığınızda size 100 GB'a kadar ücretsiz internet hediye ediyoruz\n" +
            "Şubelerimize uğrayın, danışmanlarımız aksesuar seçmenize ve yeni cihazınızı ayarlamanıza yardımcı olsun",

        "Mobil O! uygulamasındaki O!Para cüzdanını kullanıyor musunuz? Gerçekten çok pratik\n" +
            "Telefon bakiyenizden binden fazla hizmet için ödeme yapabilirsiniz; fatura, ceza ve vergiler dahil\n" +
            "QR kod ile yaptığınız her ödemede, iletişim ücreti ya da partner mağazalarda alışveriş için harcayabileceğiniz bir kazanç puanı kazanırsınız",

        "Plastik SIM kartları unutun! O! şebekesinde birkaç dakika içinde eSIM etkinleştirebilirsiniz\n" +
            "Kaybolamayan veya zarar göremeyen dijital bir karttır. Tek SIM yuvası olan iPhone'larda bile iki numara kullanmanızı sağlar\n" +
            "Uygulama üzerinden uzaktan veya herhangi bir hizmet ofisinden etkinleştirebilirsiniz",

        "O! ile dünyayı gezin. Avantajlı roaming paketlerimiz 100'den fazla ülkede geçerli\n" +
            "Bu arada, benzersiz bir hizmetimiz daha var — uçakta internet! 10 bin metre yükseklikte bile bağlantıda kalabilirsiniz\n" +
            "Uçuştan önce roaming'i açmanız yeterli, mesajlaşma uygulamalarınız tüm uçuş boyunca aktif kalır",

        "O!TV ile akıllı telefonunuzu gerçek bir sinemaya dönüştürün\n" +
            "200'den fazla yüksek kaliteli kanala ve START ile Wink dahil 5 önde gelen çevrimiçi sinema platformuna aynı anda erişebilirsiniz\n" +
            "Uygulama içinde video izlerken kullanılan tüm internet trafiği tamamen ücretsizdir ve ana paketinizden düşmez",

        "Tüm abonelerimiz için VoLTE teknolojisini devreye aldık! Artık aramalardaki ses, karşınızdaki kişi yanınızdaymış gibi son derece net\n" +
            "En büyük avantajı: görüşme sırasında internet bağlantınız kesilmiyor. Arama sırasında dosya indirmeye ya da navigasyon kullanmaya devam edebilirsiniz\n" +
            "Hizmet tamamen ücretsiz sunuluyor",

        "Numaranızı tam kontrol altına almak için Mobil O! uygulamasını yükleyin\n" +
            "Tarifenizi anında değiştirebilir, kalan GB'nizi kontrol edebilir, harcama dökümünüzü görebilir, hatta taksi çağırabilirsiniz\n" +
            "Uygulamada ayrıca ücretsiz internet paketlerinden en yeni akıllı telefonlara kadar harika ödüllü oyunlar da düzenli olarak yer alıyor",

        "Güvenliğiniz için Çocuklar Nerede hizmetini geliştirdik. Sevdiklerinizin konumunu haritada gerçek zamanlı görmenizi sağlar\n" +
            "Çocuğunuz okula vardığında veya eve döndüğünde bildirim alırsınız\n" +
            "Hizmet, çocuğunuzun telefonunda internet olmasa bile çalışır, bu da acil durumlarda oldukça güvenilir olmasını sağlar",

        "Kendinizi öne çıkarmak mı istiyorsunuz? O! ile özel bir altın numara seçebilirsiniz\n" +
            "Kolay hatırlanan rakam kombinasyonları hem iş hem de kişisel kullanım için idealdir\n" +
            "Güzel bir numarayı web sitemizden veya uygulamamızdaki Numaram bölümünden seçip rezerve edebilirsiniz",

        "O! şebekesi, Kırgızistan'da 4G kapsama alanında lider konumda. En uzak bölgelerde bile yeni baz istasyonları inşa etmeye devam ediyoruz\n" +
            "Şu anda 5G teknolojisini aktif olarak test ediyoruz, böylece abonelerimiz gelecekte yıldırım hızında internetin keyfini çıkarabilecek\n" +
            "Bizimle her zaman çevrimiçisiniz — ister şehir merkezinde ister dağların zirvesinde",
    ),

    "zh-CN" to listOf(
        "O!Store 门店目前在智能手机上有超值优惠。您可以免息、零首付分期购买任意一款设备\n" +
            "购买三星或小米智能手机，我们还会赠送最多100GB的免费流量\n" +
            "欢迎到店，我们的顾问会帮您挑选配件并设置好您的新设备",

        "您已经在用\"我的O!\"应用里的O!钱包了吗？真的非常方便\n" +
            "您可以直接用话费余额支付上千种服务，包括水电煤账单、罚款和税费\n" +
            "每次扫码支付都能获得返现，可用于支付话费或在合作商户消费",

        "告别塑料SIM卡！在O!网络下，几分钟内即可开通eSIM\n" +
            "这是一张不会丢失也不会损坏的数字卡，即使在只有一个卡槽的iPhone上也能同时使用两个号码\n" +
            "您可以在应用里远程开通，也可以到任意营业厅办理",

        "带着O!畅游世界。我们优惠的漫游套餐已覆盖100多个国家\n" +
            "对了，我们还推出了一项独家服务——机上网络！即使在一万米高空，您也能保持在线\n" +
            "只需在起飞前开启漫游，您的通讯软件将在整个飞行过程中保持畅通",

        "用O!TV把您的智能手机变成一个真正的电影院\n" +
            "您可畅享200多个高清频道，还能同时使用包括START和Wink在内的5大热门视频平台\n" +
            "应用内观看视频产生的全部流量完全免费，绝不占用您的主套餐流量",

        "我们已为所有用户开通VoLTE技术！通话音质更加清晰，仿佛对方就在您身边\n" +
            "最大的优势是——通话过程中网络不会中断，您可以边打电话边下载文件或使用导航\n" +
            "该服务完全免费",

        "安装\"我的O!\"应用，全面掌控您的号码\n" +
            "您可以随时更改套餐、查询剩余流量、查看消费明细，甚至叫车\n" +
            "应用内还经常举办抽奖活动，奖品从免费流量包到最新款智能手机应有尽有",

        "为了您的家人安全，我们推出了\"孩子在哪里\"服务，让您在地图上实时查看家人的位置\n" +
            "当孩子到达学校或回到家中时，您会收到通知提醒\n" +
            "即使孩子的手机没有开启网络，该服务依然可以正常工作，在紧急情况下非常可靠",

        "想要与众不同？在O!您可以选择专属靓号\n" +
            "好记的号码组合非常适合商务或个人使用\n" +
            "您可以直接在官网或应用的\"我的号码\"栏目中挑选并预定心仪的号码",

        "O!网络是吉尔吉斯斯坦4G覆盖的领先者，我们不断在最偏远的地区建设新的基站\n" +
            "目前我们正在积极测试5G技术，让用户未来享受到极速网络体验\n" +
            "有了我们，无论是在市中心还是身处高山之上，您都能随时保持在线",
    ),
)

package com.example.mobilka132

import com.example.mobilka132.model.BuildingInfo
import com.example.mobilka132.model.BuildingType
import com.example.mobilka132.model.VenueInfo

object CampusDatabase {
    private val buildingRegistry = mutableMapOf<Int, BuildingInfo>()

    private val CANTEEN_MENU = listOf("Борщ", "Пюре с котлетой", "Компот", "Салат Витаминный")
    private val COFFEE_MENU = listOf("Кофе Капучино", "Свежая выпечка", "Авторский чай", "Чизкейк")
    private val SHAWARMA_MENU = listOf("Шаурма классическая", "Картофель фри", "Морс")
    private val PIZZA_MENU = listOf("Пицца Пепперони", "Пицца Маргарита", "Газировка")
    private val BURGER_MENU =
        listOf("Бургер классический", "Картофель фри", "Газировка", "Крафтовое пиво")
    private val BLINY_MENU = listOf("Блины с ветчиной и сыром", "Блин сладкий", "Чай")
    private val GROCERY_MENU = listOf("Снэки", "Напитки", "Готовая еда", "Мороженое")
    private val BEER_BAR_MENU =
        listOf("Крафтовое пиво", "Гренки чесночные", "Снэки", "Бургер классический")
    private val LOUNGE_MENU = listOf("Авторский чай", "Лимонад", "Снэки", "Пицца Пепперони")
    private val ASIAN_MENU = listOf("Том Ям", "Лапша Wok", "Чай")
    private val SUSHI_MENU = listOf("Суши сет", "Лапша Wok", "Авторский чай")
    private val RESTAURANT_MENU = listOf("Салат Цезарь", "Стейк из говядины", "Вино")
    private val DESSERT_MENU = listOf("Авторский десерт", "Кофе Капучино", "Чай", "Мороженое")
    private val GEORGIAN_MENU = listOf("Хачапури", "Хинкали", "Лимонад")
    private val SEAFOOD_MENU = listOf("Мидии в соусе", "Гренки чесночные", "Вино")
    private val MEAT_SHOP_MENU = listOf("Свежее мясо", "Маринады", "Специи")
    private val VEG_SHOP_MENU = listOf("Свежие фрукты", "Свежие овощи", "Орехи")

    private const val TIME_QUICK = 15
    private const val TIME_SNACK = 30
    private const val TIME_MEAL = 60
    private const val TIME_RELAX = 120

    init {
        addBuilding(
            0xFF9511CD.toInt(), BuildingInfo(
                "Новособорная площадь", "Проспект Ленина", listOf(
                ), type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFFAED512.toInt(), BuildingInfo(
                "ТУСУР. Главный корпус", "Проспект Ленина, 40", listOf(
                    VenueInfo("Коворкинг ТУСУР", "08:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 40, coworkingComfort = 0.85)
                ), type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFF2BD936.toInt(),
            BuildingInfo(
                "Студенческий жилой комплекс «Маяк»",
                "улица Аркадия Иванова, 22, 24", listOf(
                    VenueInfo("Коворкинг Маяк", "08:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 15, coworkingComfort = 0.95)
                ),
                type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFF9FB824.toInt(),
            BuildingInfo("6 корпус ТГУ", "улица Аркадия Иванова, 49", listOf(
                VenueInfo("Коворкинг ТГУ", "09:00 - 20:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 25, coworkingComfort = 0.75)
            ), type = BuildingType.LANDMARK)
        )
        addBuilding(
            0xFF4C24BB.toInt(),
            BuildingInfo(
                "Студенческий жилой комплекс «Парус»",
                "Буяновский переулок, 3а", listOf(
                    VenueInfo("Коворкинг Парус", "08:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 20, coworkingComfort = 0.8)
                ),
                type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFFB43177.toInt(),
            BuildingInfo("5 общежитие ТГУ", "Проспект Ленина, 49а", type = BuildingType.LANDMARK)
        )
        addBuilding(
            0xFFCC203D.toInt(), BuildingInfo(
                "6 общежитие ТГУ", "Советская улица, 59", listOf(
                    VenueInfo(
                        "Столовая 'Укромное местечко'",
                        "08:00 - 16:00",
                        TIME_SNACK,
                        CANTEEN_MENU
                    )
                ), type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFFC65818.toInt(), BuildingInfo(
                "Центр культуры ТГУ", "Проспект Ленина, 36", listOf(
                    VenueInfo(
                        "Кафе-блинная 'Сибирские блины'",
                        "09:00 - 20:00",
                        TIME_SNACK,
                        BLINY_MENU
                    ),
                    VenueInfo("Кафе 'Минутка'", "09:00 - 18:00", TIME_QUICK, COFFEE_MENU),
                    VenueInfo("Столовая №1", "08:00 - 16:00", TIME_MEAL, CANTEEN_MENU)
                ), type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFF8F17BB.toInt(),
            BuildingInfo("1 Корпус ТГУ", "Проспект Ленина, 36", listOf(
                VenueInfo("Коворкинг 1 корпуса ТГУ", "09:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 30, coworkingComfort = 0.8)
            ), type = BuildingType.LANDMARK)
        )
        addBuilding(
            0xFFF32167.toInt(), BuildingInfo(
                "9 корпус ТГУ", "Проспект Ленина, 36, к9", listOf(
                    VenueInfo("Кофейня-библиотека 'Starbooks'", "08:00 - 20:00", TIME_SNACK, COFFEE_MENU),
                    VenueInfo("Коворкинг 9 корпуса ТГУ", "09:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 35, coworkingComfort = 0.9)
                ), type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFF3971D1.toInt(), BuildingInfo(
                "2 корпус ТГУ", "Проспект Ленина, 36, к2", listOf(
                    VenueInfo("Кофейня 'XO Bakery'", "08:00 - 20:00", TIME_SNACK, COFFEE_MENU),
                    VenueInfo("Коворкинг 2 корпуса ТГУ", "09:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 28, coworkingComfort = 0.78)
                ), type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFF1AE7BE.toInt(), BuildingInfo(
                "Научная библиотека ТГУ", "Проект Ленина, 34а", listOf(
                    VenueInfo("Кафе 'Научка'", "09:00 - 19:00", TIME_SNACK, COFFEE_MENU),
                    VenueInfo("Читальный зал (Коворкинг)", "08:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 50, coworkingComfort = 0.95)
                ), type = BuildingType.LANDMARK
            )
        )
        addBuilding(
            0xFF2C12D3.toInt(), BuildingInfo(
                "ТПУ. Главный корпус", "Проспект Ленина, 30", listOf(
                    VenueInfo("Столовая 'В главном'", "08:00 - 16:00", TIME_MEAL, CANTEEN_MENU),
                    VenueInfo("Коворкинг ТПУ", "08:00 - 22:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 45, coworkingComfort = 0.88)
                ), type = BuildingType.LANDMARK
            )
        )

        addBuilding(
            0xFF98F52D.toInt(),
            BuildingInfo("14 корпус ТГУ (Дом Спорта ТГУ)", "Проспект Ленина, 36", listOf(
                VenueInfo("Коворкинг 14 корпуса ТГУ", "09:00 - 20:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 20, coworkingComfort = 0.7)
            ))
        )
        addBuilding(0xFF37BC68.toInt(), BuildingInfo("4 корпус ТГУ", "Московский тракт, 8", listOf(
            VenueInfo("Коворкинг 4 корпуса ТГУ", "09:00 - 20:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 22, coworkingComfort = 0.72)
        )))
        addBuilding(0xFFE5402E.toInt(), BuildingInfo("3 корпус ТГУ", "Проспект Ленина, 34", listOf(
            VenueInfo("Коворкинг 3 корпуса ТГУ", "09:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 18, coworkingComfort = 0.68)
        )))
        addBuilding(0xFF02D9D2.toInt(), BuildingInfo("5 корпус ТГУ", "Проспект Ленина, 36 к5", listOf(
            VenueInfo("Коворкинг 5 корпуса ТГУ", "09:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 24, coworkingComfort = 0.73)
        )))
        addBuilding(
            0xFF3121C1.toInt(),
            BuildingInfo("12 корпус ТГУ", "Улица Герцена, 2 / Ново-Соборная площадь, 1 ст2", listOf(
                VenueInfo("Коворкинг 12 корпуса ТГУ", "09:00 - 21:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 32, coworkingComfort = 0.82)
            ))
        )
        addBuilding(
            0xFFF6241D.toInt(), BuildingInfo(
                "29 корпус ТГУ", "Советская улица, 46", listOf(
                    VenueInfo("Супермаркет 'Ярче'", "08:00 - 23:00", TIME_QUICK, GROCERY_MENU)
                )
            )
        )
        addBuilding(
            0xFFB98904.toInt(),
            BuildingInfo("Сибирский физико-технический институт", "Ново-Соборная площадь, 1", listOf(
                VenueInfo("Коворкинг СФТИ", "09:00 - 20:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 15, coworkingComfort = 0.65)
            ))
        )
        addBuilding(
            0xFF9CC835.toInt(), BuildingInfo(
                "СибГМУ, корпус деканатов", "Московский тракт, 2 ст20", listOf(
                    VenueInfo(
                        "Столовая 'Укромное местечко'",
                        "08:00 - 16:00",
                        TIME_MEAL,
                        CANTEEN_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF7ACE30.toInt(), BuildingInfo(
                "Томский экономико-юридический техникум", "Московский тракт, 2г", listOf(
                    VenueInfo("Кафе 'Сыр-Бор'", "09:00 - 18:00", TIME_MEAL, CANTEEN_MENU),
                    VenueInfo(
                        "Кофейня - библиотека 'Starbooks'",
                        "08:00 - 18:00",
                        TIME_SNACK,
                        COFFEE_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF37D905.toInt(), BuildingInfo(
                "5 корпус ТПУ", "Проспект Ленина, 30/2", listOf(
                    VenueInfo("Кафе 'Мини-микс'", "09:00 - 17:00", TIME_QUICK, COFFEE_MENU),
                    VenueInfo("Коворкинг 5 корпуса ТПУ", "08:00 - 22:00", 0, emptyList(), isCoworking = true, coworkingCapacity = 30, coworkingComfort = 0.8)
                )
            )
        )
        addBuilding(
            0xFFF88520.toInt(), BuildingInfo(
                "", "Московский тракт, 71а", listOf(
                    VenueInfo(
                        "Магазин еды 'Сибирский Smoker'",
                        "10:00 - 22:00",
                        TIME_QUICK,
                        MEAT_SHOP_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF4DBA25.toInt(), BuildingInfo(
                "", "Московский тракт, 46", listOf(
                    VenueInfo(
                        "Магазин пива 'Kruger Haus'",
                        "10:00 - 23:00",
                        TIME_QUICK,
                        BEER_BAR_MENU
                    ),
                    VenueInfo(
                        "Магазин пива 'Tomskoe pivo'",
                        "10:00 - 23:00",
                        TIME_QUICK,
                        BEER_BAR_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFFF8A31.toInt(), BuildingInfo(
                "", "улица Аркадия Иванова, 18а", listOf(
                    VenueInfo(
                        "Ресторанный комплекс 'У Крюгера'",
                        "12:00 - 00:00",
                        TIME_RELAX,
                        RESTAURANT_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFA6DA2C.toInt(), BuildingInfo(
                "", "Проспект Ленина, 26", listOf(
                    VenueInfo("Додо Пицца", "09:00 - 23:00", TIME_MEAL, PIZZA_MENU),
                    VenueInfo("Кафе Пряникъ", "09:00 - 21:00", TIME_SNACK, DESSERT_MENU),
                    VenueInfo(
                        "Кафе вьетнамской кухни 'Ван Куан'",
                        "10:00 - 22:00",
                        TIME_MEAL,
                        ASIAN_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFAEE52F.toInt(), BuildingInfo(
                "", "Московский тракт, 45/1", listOf(
                    VenueInfo(
                        "Магазин 'Мясной бульвар'",
                        "09:00 - 20:00",
                        TIME_QUICK,
                        MEAT_SHOP_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF24BF67.toInt(), BuildingInfo(
                "", "Московский тракт, 43/1", listOf(
                    VenueInfo(
                        "Продуктовый магазин 'Шукран'",
                        "08:00 - 22:00",
                        TIME_QUICK,
                        GROCERY_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF12E767.toInt(), BuildingInfo(
                "", "Московский тракт, 37", listOf(
                    VenueInfo(
                        "Ресторан китайской кухни 'Цзисян'",
                        "11:00 - 23:00",
                        TIME_MEAL,
                        ASIAN_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF4229E4.toInt(), BuildingInfo(
                "", "Буяновский переулок, 11а", listOf(
                    VenueInfo(
                        "Шаурмечная 'Black grill'",
                        "10:00 - 23:00",
                        TIME_SNACK,
                        SHAWARMA_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF1E77DE.toInt(), BuildingInfo(
                "", "Буяновский переулок, 12", listOf(
                    VenueInfo(
                        "Продуктовый магазин 'Подкова'",
                        "08:00 - 22:00",
                        TIME_QUICK,
                        GROCERY_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFDD813B.toInt(), BuildingInfo(
                "", "Московский тракт, 21/1", listOf(
                    VenueInfo(
                        "Шаурмечная 'Батина Шаурма'",
                        "10:00 - 23:00",
                        TIME_SNACK,
                        SHAWARMA_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF7C17BF.toInt(), BuildingInfo(
                "", "Московский тракт, 17", listOf(
                    VenueInfo("Универсам 'Абрикос'", "08:00 - 23:00", TIME_QUICK, GROCERY_MENU),
                    VenueInfo(
                        "Шаурмечная 'Безумно. Крутая шаурма'",
                        "10:00 - 23:00",
                        TIME_SNACK,
                        SHAWARMA_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF28E147.toInt(), BuildingInfo(
                "", "Московский тракт, 11Б", listOf(
                    VenueInfo(
                        "Продуктовый магазин 'Пилад'",
                        "08:00 - 22:00",
                        TIME_QUICK,
                        GROCERY_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFD81C8D.toInt(), BuildingInfo(
                "", "Московский тракт, 6/4", listOf(
                    VenueInfo("Бар 'Рюмки на стол'", "18:00 - 04:00", TIME_RELAX, BEER_BAR_MENU),
                    VenueInfo("Столовая 'Магнолия'", "08:00 - 18:00", TIME_MEAL, CANTEEN_MENU)
                )
            )
        )
        addBuilding(
            0xFF94BD0D.toInt(), BuildingInfo(
                "", "Московский тракт, 6/3", listOf(
                    VenueInfo("Магазин 'Красное&Белое'", "09:00 - 22:00", TIME_QUICK, GROCERY_MENU),
                    VenueInfo("Кафе 'У Мамы'", "09:00 - 20:00", TIME_MEAL, CANTEEN_MENU)
                )
            )
        )
        addBuilding(
            0xFFF73FCC.toInt(), BuildingInfo(
                "", "Базарный переулок, 12", listOf(
                    VenueInfo(
                        "Бар разливных напитков 'Аян'",
                        "10:00 - 23:00",
                        TIME_QUICK,
                        BEER_BAR_MENU
                    ),
                    VenueInfo("Магазин 'Паровозъ'", "10:00 - 22:00", TIME_QUICK, GROCERY_MENU),
                    VenueInfo("Супермаркет 'Ярче'", "08:00 - 22:00", TIME_QUICK, GROCERY_MENU)
                )
            )
        )
        addBuilding(
            0xFFD6D612.toInt(), BuildingInfo(
                "", "Базарный переулок, 12 киоск", listOf(
                    VenueInfo(
                        "Магазин фруктов и овощей",
                        "09:00 - 20:00",
                        TIME_QUICK,
                        VEG_SHOP_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF30C74F.toInt(), BuildingInfo(
                "", "Московский тракт, 89", listOf(
                    VenueInfo("Магазин 'Красное&Белое'", "09:00 - 22:00", TIME_QUICK, GROCERY_MENU)
                )
            )
        )
        addBuilding(
            0xFFBFA603.toInt(), BuildingInfo(
                "", "Улица Аркадия Иванова, 8", listOf(
                    VenueInfo("Лаундж-бар 'Яблунул'", "16:00 - 02:00", TIME_RELAX, LOUNGE_MENU)
                )
            )
        )
        addBuilding(
            0xFFD32A10.toInt(), BuildingInfo(
                "", "Источная улица, 44", listOf(
                    VenueInfo(
                        "Бар-пивоварня 'Beerfolio'",
                        "16:00 - 02:00",
                        TIME_RELAX,
                        BEER_BAR_MENU
                    ),
                    VenueInfo(
                        "Магазин у дома 'Бристоль'",
                        "09:00 - 22:00",
                        TIME_QUICK,
                        GROCERY_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFBA02CF.toInt(), BuildingInfo(
                "", "Источная улица, 42", listOf(
                    VenueInfo(
                        "Ирландский паб 'Harat's pub'",
                        "16:00 - 04:00",
                        TIME_RELAX,
                        BEER_BAR_MENU
                    ),
                    VenueInfo("Лаундж-бар 'Olivka'", "18:00 - 03:00", TIME_RELAX, LOUNGE_MENU)
                )
            )
        )
        addBuilding(
            0xFFB4202A.toInt(), BuildingInfo(
                "", "Московский тракт, 38а", listOf(
                    VenueInfo("Супермаркет 'Мария-Ра'", "08:00 - 22:00", TIME_QUICK, GROCERY_MENU)
                )
            )
        )
        addBuilding(
            0xFF20B1B6.toInt(), BuildingInfo(
                "", "Московский тракт, 40", listOf(
                    VenueInfo("Доставка еды 'ЕлиДаЕли'", "10:00 - 22:00", TIME_QUICK, GROCERY_MENU),
                    VenueInfo("Кафе 'Шашлычный дом'", "11:00 - 23:00", TIME_MEAL, RESTAURANT_MENU),
                    VenueInfo("Кофейня 'Бyffет'", "08:00 - 20:00", TIME_SNACK, COFFEE_MENU)
                )
            )
        )
        addBuilding(
            0xFF3AFABA.toInt(), BuildingInfo(
                "", "Московский тракт, 44а", listOf(
                    VenueInfo(
                        "Киоск фастфудной продукции 'Грузинские хачапури'",
                        "09:00 - 21:00",
                        TIME_QUICK,
                        GEORGIAN_MENU
                    ),
                    VenueInfo(
                        "Магазин фермерских продуктов",
                        "09:00 - 20:00",
                        TIME_QUICK,
                        MEAT_SHOP_MENU
                    ),
                    VenueInfo("Магазин 'Рыбка...'", "09:00 - 20:00", TIME_QUICK, SEAFOOD_MENU)
                )
            )
        )
        addBuilding(
            0xFFA431B6.toInt(), BuildingInfo(
                "", "Улица Аркадия Иванова, 37", listOf(
                    VenueInfo(
                        "Киоск по продаже овощей и фруктов",
                        "09:00 - 20:00",
                        TIME_QUICK,
                        VEG_SHOP_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFB93662.toInt(), BuildingInfo(
                "", "Улица Аркадия Иванова, 35", listOf(
                    VenueInfo("Магазин-бар 'У Ксюши'", "10:00 - 23:00", TIME_QUICK, BEER_BAR_MENU)
                )
            )
        )
        addBuilding(
            0xFF21CA70.toInt(), BuildingInfo(
                "", "Улица Аркадия Иванова, 27", listOf(
                    VenueInfo("Лаундж-бар 'Лухари'", "18:00 - 02:00", TIME_RELAX, LOUNGE_MENU),
                    VenueInfo("Ресторан доставки 'Царь'", "10:00 - 23:00", TIME_QUICK, SUSHI_MENU)
                )
            )
        )
        addBuilding(
            0xFF70E52D.toInt(), BuildingInfo(
                "", "Ново-Соборная площадь, 2а", listOf(
                    VenueInfo("Кофейня 'Mindайk coffee'", "08:00 - 22:00", TIME_SNACK, COFFEE_MENU)
                )
            )
        )
        addBuilding(
            0xFF7FB424.toInt(), BuildingInfo(
                "", "Ново-Соборная площадь, 2", listOf(
                    VenueInfo("Рестобар 'Окно'", "12:00 - 02:00", TIME_RELAX, RESTAURANT_MENU),
                    VenueInfo(
                        "Быстрое питание 'Сибирское бистро'",
                        "09:00 - 22:00",
                        TIME_SNACK,
                        BURGER_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF3ACA0E.toInt(), BuildingInfo(
                "", "Улица Герцена, 6 ст8", listOf(
                    VenueInfo(
                        "Кафе-мороженое '33 пингвина'",
                        "10:00 - 21:00",
                        TIME_QUICK,
                        DESSERT_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF13A2B8.toInt(), BuildingInfo(
                "", "Улица Герцена, 6 ст1", listOf(
                    VenueInfo(
                        "Кафе-блинная 'Сибирские блины'",
                        "09:00 - 21:00",
                        TIME_SNACK,
                        BLINY_MENU
                    ),
                    VenueInfo(
                        "Кафе быстрого питания 'Еда тут'",
                        "10:00 - 22:00",
                        TIME_QUICK,
                        BURGER_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF94CB35.toInt(), BuildingInfo(
                "", "Советская улица, 44а", listOf(
                    VenueInfo(
                        "Шашлычный двор 'Томичка'",
                        "11:00 - 23:00",
                        TIME_MEAL,
                        RESTAURANT_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFFDEB4A.toInt(), BuildingInfo(
                "", "Улица Герцена, 6/12 киоск", listOf(
                    VenueInfo(
                        "Кафе-мороженое 'Gelato Popio'",
                        "10:00 - 21:00",
                        TIME_QUICK,
                        DESSERT_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFE42148.toInt(), BuildingInfo(
                "", "Улица Герцена, 6/5, киоск", listOf(
                    VenueInfo(
                        "Магазин жареного мороженого 'Ice cool'",
                        "11:00 - 21:00",
                        TIME_QUICK,
                        DESSERT_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF37F029.toInt(), BuildingInfo(
                "", "Улица Герцена, 1а", listOf(
                    VenueInfo("Ресто-место 'Ближе'", "12:00 - 00:00", TIME_RELAX, RESTAURANT_MENU)
                )
            )
        )
        addBuilding(
            0xFF6AB41F.toInt(), BuildingInfo(
                "", "Советская улица, 47", listOf(
                    VenueInfo("Трактир 'Вечный зов'", "12:00 - 00:00", TIME_RELAX, RESTAURANT_MENU)
                )
            )
        )
        addBuilding(
            0xFFCE2468.toInt(), BuildingInfo(
                "", "Проспект Ленина, 63", listOf(
                    VenueInfo("Кофейня 'Baba Roma'", "08:00 - 22:00", TIME_SNACK, COFFEE_MENU)
                )
            )
        )
        addBuilding(
            0xFF2418D0.toInt(), BuildingInfo(
                "", "Проспект Ленина, 55", listOf(
                    VenueInfo(
                        "Семейный ресторан 'Гербарий'",
                        "11:00 - 23:00",
                        TIME_RELAX,
                        RESTAURANT_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF06E324.toInt(), BuildingInfo(
                "", "Проспект Ленина, 51а", listOf(
                    VenueInfo(
                        "Ресторан самообслуживания 'Rostic's'",
                        "10:00 - 23:00",
                        TIME_SNACK,
                        BURGER_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFB415E4.toInt(), BuildingInfo(
                "", "Советская улица, 63", listOf(
                    VenueInfo(
                        "Магазин у дома 'Бристоль'",
                        "09:00 - 22:00",
                        TIME_QUICK,
                        GROCERY_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFC10934.toInt(), BuildingInfo(
                "", "Проспект Ленина, 47а", listOf(
                    VenueInfo("Экспресс-кофейня 'Точка'", "08:00 - 21:00", TIME_QUICK, COFFEE_MENU)
                )
            )
        )
        addBuilding(
            0xFFE1DA16.toInt(), BuildingInfo(
                "", "Улица Усова, 3", listOf(
                    VenueInfo(
                        "Кафе быстрого питания 'Прожарка'",
                        "10:00 - 22:00",
                        TIME_SNACK,
                        BURGER_MENU
                    ),
                    VenueInfo(
                        "Экспресс-кофейня 'Ракета Кофе'",
                        "08:00 - 21:00",
                        TIME_QUICK,
                        COFFEE_MENU
                    ),
                    VenueInfo("Кофейня 'Baba Roma'", "08:00 - 22:00", TIME_SNACK, COFFEE_MENU)
                )
            )
        )
        addBuilding(
            0xFFB623B1.toInt(), BuildingInfo(
                "", "Проспект Ленина, 41", listOf(
                    VenueInfo("Супермаркет 'Пятёрочка'", "08:00 - 23:00", TIME_QUICK, GROCERY_MENU),
                    VenueInfo("Магазин 'Panda'", "09:00 - 21:00", TIME_QUICK, GROCERY_MENU),
                    VenueInfo(
                        "Ирландский паб 'Клевер'",
                        "16:00 - 02:00",
                        TIME_RELAX,
                        BEER_BAR_MENU
                    ),
                    VenueInfo("Пати-бар 'Лайт'", "18:00 - 04:00", TIME_RELAX, LOUNGE_MENU),
                    VenueInfo("Лаундж-бар 'Хука'", "16:00 - 02:00", TIME_RELAX, LOUNGE_MENU),
                    VenueInfo(
                        "Кафе-блинная 'Сибирские блины'",
                        "09:00 - 21:00",
                        TIME_SNACK,
                        BLINY_MENU
                    ),
                    VenueInfo(
                        "Кафе быстрого питания 'Doner master'",
                        "10:00 - 23:00",
                        TIME_QUICK,
                        SHAWARMA_MENU
                    ),
                    VenueInfo(
                        "Кофейня 'YourTime Specialty'",
                        "08:00 - 21:00",
                        TIME_SNACK,
                        COFFEE_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFF422AE.toInt(), BuildingInfo(
                "", "Улица Усова, 1/1", listOf(
                    VenueInfo("Бар 'Custom street'", "18:00 - 04:00", TIME_RELAX, BEER_BAR_MENU)
                )
            )
        )
        addBuilding(
            0xFFF2DF38.toInt(), BuildingInfo(
                "", "Проспект Ленина, 37", listOf(
                    VenueInfo("Кофейни 'Paradox coffee'", "08:00 - 22:00", TIME_SNACK, COFFEE_MENU)
                )
            )
        )
        addBuilding(
            0xFF82EE06.toInt(), BuildingInfo(
                "", "Улица Герцена, 5", listOf(
                    VenueInfo(
                        "Кафе-кондитерская 'Пеки, Лола'",
                        "09:00 - 21:00",
                        TIME_SNACK,
                        DESSERT_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF1760F2.toInt(), BuildingInfo(
                "", "Советская улица, 50", listOf(
                    VenueInfo(
                        "Продуктовый магазин 'Наш гастроном'",
                        "08:00 - 22:00",
                        TIME_QUICK,
                        GROCERY_MENU
                    ),
                    VenueInfo(
                        "Кофейня самообслуживания 'Мойё кофе'",
                        "00:00 - 24:00",
                        TIME_QUICK,
                        COFFEE_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF455EFD.toInt(), BuildingInfo(
                "Арбитражный суд Томской области", "Проспект Кирова, 10", listOf(
                    VenueInfo("Столовые 'Уютный уголок'", "09:00 - 17:00", TIME_MEAL, CANTEEN_MENU)
                )
            )
        )
        addBuilding(
            0xFFE81CCD.toInt(), BuildingInfo(
                "", "Улица Усова, 6 ст12", listOf(
                    VenueInfo(
                        "Сеть кафе уютной кухни 'Колобок. Ру'",
                        "09:00 - 21:00",
                        TIME_MEAL,
                        CANTEEN_MENU
                    ),
                    VenueInfo("Кафе 'Yoki Toki'", "11:00 - 23:00", TIME_MEAL, ASIAN_MENU),
                    VenueInfo("Кафе 'Ассорти'", "10:00 - 22:00", TIME_MEAL, CANTEEN_MENU)
                )
            )
        )
        addBuilding(
            0xFF4133BE.toInt(), BuildingInfo(
                "", "Проспект Кирова, 3г", listOf(
                    VenueInfo("Супермаркет 'Ярче'", "08:00 - 23:00", TIME_QUICK, GROCERY_MENU)
                )
            )
        )
        addBuilding(
            0xFFFE18B9.toInt(), BuildingInfo(
                "", "Проспект Кирова, 5а", listOf(
                    VenueInfo("Пиццерия 'Папа Джонс'", "10:00 - 23:00", TIME_MEAL, PIZZA_MENU),
                    VenueInfo("Кофейня 'Буланже'", "08:00 - 22:00", TIME_SNACK, COFFEE_MENU)
                )
            )
        )
        addBuilding(
            0xFFEE0DA3.toInt(), BuildingInfo(
                "", "Проспект Кирова, 5 ст8", listOf(
                    VenueInfo(
                        "Пекарня-кондитерская 'Тесто'",
                        "08:00 - 21:00",
                        TIME_QUICK,
                        COFFEE_MENU
                    )
                )
            )
        )

        addBuilding(
            0xFF7DC139.toInt(), BuildingInfo(
                "Лампочка", "Проспект Кирова, 5 ст13", listOf(
                    VenueInfo("Стейк-хаус 'Антрекот'", "10:00 - 22:00", TIME_MEAL, RESTAURANT_MENU),
                    VenueInfo(
                        "Азиатский стритфуд 'Noods'",
                        "10:00 - 22:00",
                        TIME_SNACK,
                        ASIAN_MENU
                    ),
                    VenueInfo(
                        "Быстрое питание 'Безумно. Крутая шаурма'",
                        "10:00 - 22:00",
                        TIME_SNACK,
                        SHAWARMA_MENU
                    ),
                    VenueInfo("Бары 'Завод'", "12:00 - 00:00", TIME_RELAX, BEER_BAR_MENU),
                    VenueInfo("Пиццерия 'Мэйк лав пицца'", "10:00 - 22:00", TIME_MEAL, PIZZA_MENU),
                    VenueInfo(
                        "Центры паровых коктейлей 'Edison'",
                        "12:00 - 02:00",
                        TIME_RELAX,
                        LOUNGE_MENU
                    ),
                    VenueInfo(
                        "Кофе-кондитерская 'Торта'",
                        "10:00 - 22:00",
                        TIME_SNACK,
                        DESSERT_MENU
                    ),
                    VenueInfo(
                        "Быстрое питание 'Мидийная'",
                        "10:00 - 22:00",
                        TIME_MEAL,
                        SEAFOOD_MENU
                    ),
                    VenueInfo(
                        "Быстрое питание 'Vaffel'",
                        "10:00 - 22:00",
                        TIME_SNACK,
                        DESSERT_MENU
                    ),
                    VenueInfo(
                        "Вьетнамское стритфуд-кафе 'Нам Фо'",
                        "10:00 - 22:00",
                        TIME_MEAL,
                        ASIAN_MENU
                    ),
                    VenueInfo("Пастерия 'Три сыра'", "10:00 - 22:00", TIME_MEAL, PIZZA_MENU),
                    VenueInfo(
                        "Быстрое питание 'Большой Ребровски'",
                        "10:00 - 22:00",
                        TIME_MEAL,
                        BURGER_MENU
                    ),
                    VenueInfo("Кафе-Бар 'Unami'", "10:00 - 22:00", TIME_MEAL, ASIAN_MENU),
                    VenueInfo("Кафе 'Рис да Барбарис'", "10:00 - 22:00", TIME_MEAL, ASIAN_MENU),
                    VenueInfo(
                        "Кафе-бар мексиканской кухни 'Chupito'",
                        "10:00 - 22:00",
                        TIME_SNACK,
                        BURGER_MENU
                    ),
                    VenueInfo(
                        "Кондитерские изделия 'Мотимания'",
                        "10:00 - 22:00",
                        TIME_QUICK,
                        DESSERT_MENU
                    ),
                    VenueInfo(
                        "Быстрое питание 'У Нино'",
                        "10:00 - 22:00",
                        TIME_MEAL,
                        GEORGIAN_MENU
                    ),
                    VenueInfo(
                        "Кафе Бурятской кухни 'Бууза'",
                        "10:00 - 22:00",
                        TIME_MEAL,
                        ASIAN_MENU
                    ),
                    VenueInfo(
                        "Кофейни 'Surf Coffee x Lamp'",
                        "10:00 - 22:00",
                        TIME_SNACK,
                        COFFEE_MENU
                    ),
                    VenueInfo("Кафе 'Moments'", "10:00 - 22:00", TIME_SNACK, COFFEE_MENU),
                    VenueInfo("Кафе 'Тот самый драник'", "10:00 - 22:00", TIME_MEAL, CANTEEN_MENU),
                    VenueInfo(
                        "Кондитерские 'Могу себе позволить'",
                        "10:00 - 22:00",
                        TIME_QUICK,
                        DESSERT_MENU
                    ),
                    VenueInfo(
                        "Точка продажи напитков 'Шейк'",
                        "10:00 - 22:00",
                        TIME_QUICK,
                        LOUNGE_MENU
                    ),
                    VenueInfo(
                        "Кафе-пиццерия 'Give Me Two'",
                        "10:00 - 22:00",
                        TIME_MEAL,
                        PIZZA_MENU
                    ),
                    VenueInfo("Чайная 'Заварка'", "10:00 - 22:00", TIME_RELAX, LOUNGE_MENU),
                    VenueInfo(
                        "Центры паровых коктейлей 'Terra'",
                        "12:00 - 02:00",
                        TIME_RELAX,
                        LOUNGE_MENU
                    ),
                    VenueInfo(
                        "Киоск мороженого '33 пингвина'",
                        "10:00 - 22:00",
                        TIME_QUICK,
                        DESSERT_MENU
                    ),
                    VenueInfo("Бары 'Центральный бар'", "12:00 - 00:00", TIME_RELAX, BEER_BAR_MENU),
                    VenueInfo("Бары 'Beer point'", "12:00 - 00:00", TIME_RELAX, BEER_BAR_MENU)
                )
            )
        )

        addBuilding(
            0xFF2B5DD1.toInt(), BuildingInfo(
                "", "Проспект Кирова, 5/4", listOf(
                    VenueInfo("Бары 'Настроение'", "18:00 - 04:00", TIME_RELAX, LOUNGE_MENU)
                )
            )
        )
        addBuilding(
            0xFFB44B31.toInt(), BuildingInfo(
                "The Елань", "Советская улица, 78", listOf(
                    VenueInfo(
                        "Рестораны 'Poly bistro'",
                        "12:00 - 00:00",
                        TIME_RELAX,
                        RESTAURANT_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFFF93E95.toInt(), BuildingInfo(
                "", "Советская улица, 80", listOf(
                    VenueInfo(
                        "Кафе - кондитерская 'Кудесы'",
                        "09:00 - 21:00",
                        TIME_SNACK,
                        DESSERT_MENU
                    ),
                    VenueInfo("Кафе 'Пешком постою'", "11:00 - 23:00", TIME_MEAL, GEORGIAN_MENU),
                    VenueInfo("Гриль-бар 'Rebro'", "12:00 - 00:00", TIME_MEAL, BURGER_MENU),
                    VenueInfo(
                        "Китайский ресторан 'Panda Jui'",
                        "11:00 - 23:00",
                        TIME_MEAL,
                        ASIAN_MENU
                    )
                )
            )
        )
        addBuilding(
            0xFF04EB60.toInt(), BuildingInfo(
                "", "Улица Усова, 6 ст13", listOf(
                    VenueInfo("Лаундж-бар 'Malevich'", "16:00 - 04:00", TIME_RELAX, LOUNGE_MENU),
                    VenueInfo("Магазин-бар 'За пивком'", "10:00 - 02:00", TIME_QUICK, BEER_BAR_MENU)
                )
            )
        )
        addBuilding(
            0xFFCE9728.toInt(), BuildingInfo(
                "", "Улица Усова, 9Б", listOf(
                    VenueInfo(
                        "Экспресс-кофейня 'Территория Кофе'",
                        "08:00 - 21:00",
                        TIME_QUICK,
                        COFFEE_MENU
                    )
                )
            )
        )
    }

    fun getBuildingByColor(color: Int): BuildingInfo? {
        return buildingRegistry[color and 0x00FFFFFF]
    }

    fun getAllBuildings(): Map<Int, BuildingInfo> = buildingRegistry

    private fun addBuilding(color: Int, info: BuildingInfo) {
        buildingRegistry[color and 0x00FFFFFF] = info
    }
}

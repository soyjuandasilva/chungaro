package com.chungaro.combustible.data

/** Tipo de vehículo con su emoji y un consumo típico (L/100 km) para sugerir. */
data class VehicleType(val id: String, val label: String, val emoji: String, val typicalConsumption: Double)

val VEHICLE_TYPES = listOf(
    VehicleType("car", "Coche", "🚗", 6.5),
    VehicleType("moto", "Moto", "🏍️", 4.0),
    VehicleType("van", "Furgoneta", "🚐", 8.5),
    VehicleType("truck", "Camión", "🚚", 28.0),
    VehicleType("camper", "Autocaravana", "🏕️", 11.0),
    VehicleType("other", "Otro", "🚙", 7.0),
)

fun vehicleType(id: String): VehicleType = VEHICLE_TYPES.firstOrNull { it.id == id } ?: VEHICLE_TYPES.first()

val Vehicle.emoji: String get() = vehicleType(type).emoji

/** "Renault Clio · 2019" o vacío. */
val Vehicle.description: String
    get() = listOf(listOf(brand, model).filter { it.isNotBlank() }.joinToString(" "), year?.toString().orEmpty())
        .filter { it.isNotBlank() }
        .joinToString(" · ")

/** Marcas y modelos frecuentes en Portugal, para sugerir (se puede escribir cualquier otro). */
object VehicleCatalog {
    private val cars = mapOf(
        "Renault" to listOf("Clio", "Captur", "Mégane", "Austral", "Arkana", "Kadjar", "Twingo", "Zoe", "Scénic"),
        "Peugeot" to listOf("208", "2008", "308", "3008", "5008", "408", "Rifter"),
        "Citroën" to listOf("C3", "C3 Aircross", "C4", "C5 Aircross", "Berlingo", "C1"),
        "Volkswagen" to listOf("Polo", "Golf", "T-Roc", "T-Cross", "Tiguan", "Passat", "Taigo", "ID.3", "ID.4"),
        "Seat" to listOf("Ibiza", "Leon", "Arona", "Ateca", "Tarraco"),
        "Cupra" to listOf("Formentor", "Born", "Leon"),
        "Toyota" to listOf("Yaris", "Yaris Cross", "Corolla", "C-HR", "RAV4", "Aygo X", "Auris"),
        "Mercedes-Benz" to listOf("Classe A", "Classe B", "Classe C", "Classe E", "CLA", "GLA", "GLC"),
        "BMW" to listOf("Série 1", "Série 2", "Série 3", "Série 5", "X1", "X2", "X3", "X5", "i4"),
        "Audi" to listOf("A1", "A3", "A4", "A6", "Q2", "Q3", "Q5"),
        "Fiat" to listOf("500", "Panda", "Tipo", "Punto", "500X"),
        "Opel" to listOf("Corsa", "Astra", "Mokka", "Crossland", "Grandland"),
        "Dacia" to listOf("Sandero", "Duster", "Jogger", "Spring", "Logan"),
        "Nissan" to listOf("Micra", "Juke", "Qashqai", "X-Trail", "Leaf"),
        "Kia" to listOf("Picanto", "Rio", "Ceed", "Stonic", "Sportage", "Niro"),
        "Hyundai" to listOf("i10", "i20", "i30", "Bayon", "Kona", "Tucson"),
        "Ford" to listOf("Fiesta", "Focus", "Puma", "Kuga", "Mondeo"),
        "Skoda" to listOf("Fabia", "Scala", "Octavia", "Kamiq", "Karoq", "Kodiaq"),
        "Mazda" to listOf("2", "3", "CX-3", "CX-30", "CX-5"),
        "Honda" to listOf("Jazz", "Civic", "HR-V", "CR-V"),
        "Volvo" to listOf("XC40", "XC60", "XC90", "V40", "V60"),
        "Tesla" to listOf("Model 3", "Model Y"),
        "MG" to listOf("ZS", "MG4", "HS"),
        "Mitsubishi" to listOf("Space Star", "ASX", "Outlander"),
        "Suzuki" to listOf("Swift", "Ignis", "Vitara", "S-Cross"),
        "Mini" to listOf("Cooper", "Countryman"),
        "Smart" to listOf("Fortwo", "Forfour"),
        "Jeep" to listOf("Renegade", "Compass", "Avenger"),
        "Land Rover" to listOf("Defender", "Discovery Sport", "Range Rover Evoque"),
        "Porsche" to listOf("Macan", "Cayenne", "911"),
    )
    private val motos = mapOf(
        "Honda" to listOf("PCX 125", "SH 125", "Forza 125", "CB500F", "CB650R", "NC750X", "Africa Twin"),
        "Yamaha" to listOf("NMAX 125", "XMAX 125", "XMAX 300", "MT-07", "MT-09", "Tracer 9", "Ténéré 700"),
        "Kawasaki" to listOf("Z650", "Z900", "Versys 650", "Ninja 650"),
        "Suzuki" to listOf("Burgman 125", "GSX-S750", "V-Strom 650"),
        "BMW" to listOf("R 1250 GS", "F 900 R", "C 400 X", "G 310 R"),
        "Piaggio" to listOf("Liberty 125", "Beverly 300", "MP3"),
        "Vespa" to listOf("Primavera 125", "GTS 300"),
        "KTM" to listOf("Duke 125", "Duke 390", "890 Adventure"),
        "Kymco" to listOf("Agility 125", "People S 125", "AK 550"),
        "SYM" to listOf("Symphony 125", "Jet 14", "Joymax Z"),
        "Ducati" to listOf("Monster", "Scrambler", "Multistrada"),
        "Triumph" to listOf("Trident 660", "Street Triple", "Tiger 900"),
        "Harley-Davidson" to listOf("Sportster", "Street Glide"),
        "Zontes" to listOf("ZT125", "ZT350"),
    )
    private val vans = mapOf(
        "Renault" to listOf("Kangoo", "Trafic", "Master"),
        "Peugeot" to listOf("Partner", "Expert", "Boxer"),
        "Citroën" to listOf("Berlingo", "Jumpy", "Jumper"),
        "Volkswagen" to listOf("Caddy", "Transporter", "Crafter"),
        "Ford" to listOf("Transit Connect", "Transit Custom", "Transit"),
        "Mercedes-Benz" to listOf("Citan", "Vito", "Sprinter"),
        "Fiat" to listOf("Doblò", "Scudo", "Ducato"),
        "Opel" to listOf("Combo", "Vivaro", "Movano"),
        "Toyota" to listOf("Proace City", "Proace", "Hilux"),
        "Nissan" to listOf("Townstar", "Primastar", "Interstar"),
        "Iveco" to listOf("Daily"),
    )
    private val trucks = mapOf(
        "Mercedes-Benz" to listOf("Actros", "Atego", "Arocs"),
        "Volvo" to listOf("FH", "FM", "FL"),
        "Scania" to listOf("Série R", "Série S", "Série P"),
        "MAN" to listOf("TGX", "TGS", "TGL"),
        "DAF" to listOf("XF", "XG", "CF", "LF"),
        "Iveco" to listOf("S-Way", "Eurocargo"),
        "Renault Trucks" to listOf("T", "D", "C"),
    )
    private val campers = mapOf(
        "Fiat" to listOf("Ducato"),
        "Volkswagen" to listOf("California", "Grand California"),
        "Mercedes-Benz" to listOf("Marco Polo", "Sprinter"),
        "Ford" to listOf("Transit Custom Nugget"),
        "Hymer" to listOf("B-Class", "Free"),
        "Adria" to listOf("Twin", "Coral"),
    )

    private fun table(type: String): Map<String, List<String>> = when (type) {
        "moto" -> motos
        "van" -> vans
        "truck" -> trucks
        "camper" -> campers
        else -> cars
    }

    fun brands(type: String): List<String> = table(type).keys.sorted()

    fun models(type: String, brand: String): List<String> =
        table(type).entries.firstOrNull { it.key.equals(brand.trim(), ignoreCase = true) }?.value.orEmpty()
}

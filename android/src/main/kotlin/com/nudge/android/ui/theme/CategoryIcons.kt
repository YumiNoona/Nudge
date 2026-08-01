package com.nudge.android.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconOption(val key: String, val label: String, val image: ImageVector)

/** A single, searchable Material Rounded icon vocabulary for every category surface. */
object CategoryIcons {
    val all = listOf(
        CategoryIconOption("restaurant", "Restaurant", Icons.Rounded.Restaurant),
        CategoryIconOption("fastfood", "Fast food", Icons.Rounded.Fastfood),
        CategoryIconOption("cafe", "Cafe", Icons.Rounded.LocalCafe),
        CategoryIconOption("bar", "Bar", Icons.Rounded.LocalBar),
        CategoryIconOption("cake", "Cake", Icons.Rounded.Cake),
        CategoryIconOption("bakery", "Bakery", Icons.Rounded.BakeryDining),
        CategoryIconOption("lunch", "Lunch", Icons.Rounded.LunchDining),
        CategoryIconOption("dinner", "Dinner", Icons.Rounded.DinnerDining),
        CategoryIconOption("icecream", "Ice cream", Icons.Rounded.Icecream),
        CategoryIconOption("beverage", "Beverage", Icons.Rounded.EmojiFoodBeverage),
        CategoryIconOption("car", "Car", Icons.Rounded.DirectionsCar),
        CategoryIconOption("bus", "Bus", Icons.Rounded.DirectionsBus),
        CategoryIconOption("train", "Train", Icons.Rounded.Train),
        CategoryIconOption("flight", "Flight", Icons.Rounded.Flight),
        CategoryIconOption("bike", "Motorbike", Icons.Rounded.TwoWheeler),
        CategoryIconOption("cycle", "Bicycle", Icons.Rounded.PedalBike),
        CategoryIconOption("taxi", "Taxi", Icons.Rounded.LocalTaxi),
        CategoryIconOption("commute", "Commute", Icons.Rounded.Commute),
        CategoryIconOption("fuel", "Fuel", Icons.Rounded.LocalGasStation),
        CategoryIconOption("charging", "Charging", Icons.Rounded.EvStation),
        CategoryIconOption("cart", "Cart", Icons.Rounded.ShoppingCart),
        CategoryIconOption("bag", "Shopping bag", Icons.Rounded.ShoppingBag),
        CategoryIconOption("store", "Store", Icons.Rounded.Storefront),
        CategoryIconOption("clothes", "Clothing", Icons.Rounded.Checkroom),
        CategoryIconOption("mall", "Mall", Icons.Rounded.LocalMall),
        CategoryIconOption("gift", "Gift", Icons.Rounded.Redeem),
        CategoryIconOption("sale", "Sale", Icons.Rounded.Sell),
        CategoryIconOption("inventory", "Inventory", Icons.Rounded.Inventory2),
        CategoryIconOption("home", "Home", Icons.Rounded.Home),
        CategoryIconOption("apartment", "Apartment", Icons.Rounded.Apartment),
        CategoryIconOption("bed", "Bedroom", Icons.Rounded.Bed),
        CategoryIconOption("chair", "Furniture", Icons.Rounded.Chair),
        CategoryIconOption("kitchen", "Kitchen", Icons.Rounded.Kitchen),
        CategoryIconOption("shower", "Bathroom", Icons.Rounded.Shower),
        CategoryIconOption("weekend", "Weekend", Icons.Rounded.Weekend),
        CategoryIconOption("electricity", "Electricity", Icons.Rounded.ElectricalServices),
        CategoryIconOption("light", "Lighting", Icons.Rounded.Lightbulb),
        CategoryIconOption("water", "Water", Icons.Rounded.WaterDrop),
        CategoryIconOption("wifi", "Internet", Icons.Rounded.Wifi),
        CategoryIconOption("phone", "Phone", Icons.Rounded.PhoneAndroid),
        CategoryIconOption("cable", "Cable", Icons.Rounded.Cable),
        CategoryIconOption("power", "Power", Icons.Rounded.Power),
        CategoryIconOption("health", "Health", Icons.Rounded.HealthAndSafety),
        CategoryIconOption("medical", "Medical", Icons.Rounded.MedicalServices),
        CategoryIconOption("hospital", "Hospital", Icons.Rounded.LocalHospital),
        CategoryIconOption("medicine", "Medicine", Icons.Rounded.Medication),
        CategoryIconOption("fitness", "Fitness", Icons.Rounded.FitnessCenter),
        CategoryIconOption("spa", "Personal care", Icons.Rounded.Spa),
        CategoryIconOption("school", "School", Icons.Rounded.School),
        CategoryIconOption("book", "Books", Icons.Rounded.MenuBook),
        CategoryIconOption("computer", "Computer", Icons.Rounded.Computer),
        CategoryIconOption("science", "Science", Icons.Rounded.Science),
        CategoryIconOption("calculate", "Fees", Icons.Rounded.Calculate),
        CategoryIconOption("movie", "Movies", Icons.Rounded.Movie),
        CategoryIconOption("music", "Music", Icons.Rounded.MusicNote),
        CategoryIconOption("gaming", "Gaming", Icons.Rounded.SportsEsports),
        CategoryIconOption("football", "Football", Icons.Rounded.SportsSoccer),
        CategoryIconOption("cricket", "Cricket", Icons.Rounded.SportsCricket),
        CategoryIconOption("camera", "Photography", Icons.Rounded.CameraAlt),
        CategoryIconOption("art", "Art", Icons.Rounded.Palette),
        CategoryIconOption("theatre", "Theatre", Icons.Rounded.TheaterComedy),
        CategoryIconOption("work", "Work", Icons.Rounded.Work),
        CategoryIconOption("business", "Business", Icons.Rounded.BusinessCenter),
        CategoryIconOption("payments", "Payments", Icons.Rounded.Payments),
        CategoryIconOption("bank", "Bank", Icons.Rounded.AccountBalance),
        CategoryIconOption("savings", "Savings", Icons.Rounded.Savings),
        CategoryIconOption("investment", "Investment", Icons.Rounded.TrendingUp),
        CategoryIconOption("rupee", "Income", Icons.Rounded.CurrencyRupee),
        CategoryIconOption("pets", "Pets", Icons.Rounded.Pets),
        CategoryIconOption("child", "Child care", Icons.Rounded.ChildCare),
        CategoryIconOption("family", "Family", Icons.Rounded.FamilyRestroom),
        CategoryIconOption("personal", "Personal", Icons.Rounded.Face),
        CategoryIconOption("mindfulness", "Mindfulness", Icons.Rounded.SelfImprovement),
        CategoryIconOption("travel", "Travel", Icons.Rounded.TravelExplore),
        CategoryIconOption("luggage", "Luggage", Icons.Rounded.Luggage),
        CategoryIconOption("beach", "Beach", Icons.Rounded.BeachAccess),
        CategoryIconOption("hotel", "Hotel", Icons.Rounded.Hotel),
        CategoryIconOption("map", "Map", Icons.Rounded.Map),
        CategoryIconOption("subscriptions", "Subscriptions", Icons.Rounded.Subscriptions),
        CategoryIconOption("notification", "Notifications", Icons.Rounded.Notifications),
        CategoryIconOption("cloud", "Cloud", Icons.Rounded.Cloud),
        CategoryIconOption("security", "Security", Icons.Rounded.Security),
        CategoryIconOption("key", "Key", Icons.Rounded.Key),
        CategoryIconOption("lock", "Lock", Icons.Rounded.Lock),
        CategoryIconOption("construction", "Construction", Icons.Rounded.Construction),
        CategoryIconOption("repair", "Repair", Icons.Rounded.Build),
        CategoryIconOption("handyman", "Handyman", Icons.Rounded.Handyman),
        CategoryIconOption("cleaning", "Cleaning", Icons.Rounded.CleaningServices),
        CategoryIconOption("farming", "Farming", Icons.Rounded.Agriculture),
        CategoryIconOption("eco", "Eco", Icons.Rounded.Eco),
        CategoryIconOption("park", "Park", Icons.Rounded.Park),
        CategoryIconOption("forest", "Forest", Icons.Rounded.Forest),
        CategoryIconOption("recycle", "Recycling", Icons.Rounded.Recycling),
        CategoryIconOption("favorite", "Favorite", Icons.Rounded.Favorite),
        CategoryIconOption("star", "Star", Icons.Rounded.Star),
        CategoryIconOption("celebrate", "Celebration", Icons.Rounded.Celebration),
        CategoryIconOption("donation", "Donation", Icons.Rounded.VolunteerActivism),
        CategoryIconOption("receipt", "Receipt", Icons.Rounded.ReceiptLong),
        CategoryIconOption("tag", "Tag", Icons.Rounded.LocalOffer),
        CategoryIconOption("category", "Category", Icons.Rounded.Category),
        CategoryIconOption("more", "Other", Icons.Rounded.MoreHoriz),
        CategoryIconOption("salary", "Salary", Icons.Rounded.RequestQuote),
        CategoryIconOption("refund", "Refund", Icons.Rounded.Replay),
        CategoryIconOption("cash", "Cash", Icons.Rounded.Money),
        CategoryIconOption("card", "Card", Icons.Rounded.CreditCard),
        CategoryIconOption("upi", "UPI", Icons.Rounded.QrCode2),
        CategoryIconOption("insurance", "Insurance", Icons.Rounded.Policy),
        CategoryIconOption("tax", "Tax", Icons.Rounded.Percent),
        CategoryIconOption("baby", "Baby", Icons.Rounded.ChildFriendly),
        CategoryIconOption("laundry", "Laundry", Icons.Rounded.LocalLaundryService),
        CategoryIconOption("print", "Printing", Icons.Rounded.Print),
        CategoryIconOption("package", "Package", Icons.Rounded.Inventory),
        CategoryIconOption("charity", "Charity", Icons.Rounded.Handshake),
        CategoryIconOption("wallet", "Wallet", Icons.Rounded.AccountBalanceWallet)
    )

    fun resolve(key: String?, categoryName: String = ""): ImageVector {
        all.firstOrNull { it.key == key }?.let { return it.image }
        val name = categoryName.lowercase()
        val inferred = when {
            "food" in name || "dining" in name -> "restaurant"
            "transport" in name -> "commute"
            "grocery" in name -> "cart"
            "shopping" in name -> "bag"
            "entertain" in name -> "movie"
            "utilit" in name -> "electricity"
            "rent" in name || "housing" in name -> "home"
            "health" in name -> "health"
            "education" in name -> "school"
            "subscription" in name -> "subscriptions"
            "travel" in name -> "travel"
            "personal" in name -> "spa"
            "gift" in name -> "gift"
            "invest" in name || "interest" in name -> "investment"
            "salary" in name -> "salary"
            "freelance" in name -> "work"
            "refund" in name -> "refund"
            else -> "category"
        }
        return all.first { it.key == inferred }.image
    }
}

@Composable
fun CategoryGlyph(
    iconKey: String?,
    categoryName: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = CategoryIcons.resolve(iconKey, categoryName),
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

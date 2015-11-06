package sample;

import com.firebase.client.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import sun.misc.BASE64Decoder;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

import static java.lang.Thread.sleep;

public class Controller implements Initializable {

    //connects the ids from GUI
    @FXML public TextField countdown;
    @FXML public TextArea itemDescription;
    @FXML public TextArea bidHistory;
    @FXML public TextField highestBid;
    @FXML public ImageView itemImage;
    @FXML public Button button;
    @FXML public javafx.scene.control.Label pincode;




    //Creates a connection to Firebase Items
    private Firebase myFirebase = new Firebase("https://biddme.firebaseio.com/items");
    private Firebase firebaseRef = new Firebase("https://biddme.firebaseio.com/");

    private Firebase myFirebaseUsers = new Firebase("https://biddme.firebaseio.com/users");

    //Creates a two lists which holds the FirebaseItems and FirebaseUsers
    private List<BidItem> fireBaseItems = new ArrayList<BidItem>();
    private List<BidUsers> fireBaseUsers = new ArrayList<BidUsers>();
    private  Map<String, Object> deactivate= new HashMap<String, Object>();
    private  Map<String, Object> activate= new HashMap<String, Object>();


    private int highestBidder = 0;
    private int latestBid = 0;
    private String idHighestBidder;
    private String idBuyer;


    private HashMap<String, BidItem> itemsMap = new HashMap<String, BidItem>();

    //Array to store all bids
    private ArrayList<String> allBids = new ArrayList();

    private static int seconds;
    private int TIME = 30;

    private String titleName;
    private String buyerId;
    private String itemId;
    private Image newImage;
    private String imageString;
    private String description;
    private String type;
    private String formattedBidArray;
    private Integer startPrice;
    private String seller;

    private HashMap<String, Object> hashMapItem;

    //Initialize the program
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        goOnline();
        System.out.println("Initialize data...");
        hashMapItem = new HashMap<String, Object>();
        setImageBid();

        int pin = (int)(Math.random() * 9999)+1000;
        firebaseRef.child("pincode").setValue(pin);
        pincode.setText("PINCODE: " + pin);

        seconds = TIME;

        myFirebaseUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                System.out.println("There are " + dataSnapshot.getChildrenCount() + " users");
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    BidUsers bidUsers = postSnapshot.getValue(BidUsers.class);
                    fireBaseUsers.add(bidUsers);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        myFirebase.addChildEventListener(new ChildEventListener() {
            // Retrieve new posts as they are added to the database
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                /*Selects only a value which will be used
                    H�mtar title och l�gger i itemsMap*/
                BidItem bidItem = snapshot.getValue(BidItem.class);
                fireBaseItems.add(bidItem);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                final BidItem bidItem = dataSnapshot.getValue(BidItem.class);
                fireBaseItems.add(bidItem);
                System.out.println("Child updated");

                if (bidItem.getUpForSale() && !bidItem.isSold() && seconds == TIME) {
                    updateDescription(bidItem);
                    System.out.println("Update Timer");
                    final Timer timer = new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            countdown.setText("Time left: " + --seconds + " seconds");
                            if (seconds == 0) {
                                System.out.println("Entered seconds ==0 if sats");
                                timer.cancel();
                                if (bidItem.getCurrentPrice() != bidItem.getStartedPrice()){
                                    System.out.println("Item sold!");
                                    idBuyer = idHighestBidder;
                                    myFirebase.child(bidItem.getId()).child("idBuyer").setValue(idBuyer);
                                    System.out.println(idBuyer + " Won the auction");
                                    pastItemSold();
                                }
                                else{
                                    System.out.println("Item not sold");
                                }
                                pastItemForSale();
                                clearGUI();

                                fireBaseItems.remove(0);
                            }
                        }
                    }, 1000, 1000);
                }

                try {
                    for (int i = 0; i < fireBaseUsers.size(); i++) {

                        if (bidItem.getBids().get(fireBaseUsers.get(i).getId()) != null) {
                            latestBid = bidItem.getBids().get(fireBaseUsers.get(i).getId());
                            idBuyer = fireBaseUsers.get(i).getId();

                            bidHistory.setText("bidds " + fireBaseUsers.get(i).getUsername() + " with amount of " +latestBid);


                            if (latestBid > highestBidder) {
                                highestBidder = latestBid;
                                highestBid.setText(fireBaseUsers.get(i).getUsername() + " with amount of " + highestBidder);
                                idHighestBidder = fireBaseUsers.get(i).getId();
                                System.out.println("leading the auction: "+idHighestBidder);
                                String newBid = String.valueOf(highestBidder);
                                allBids.add("    "+ newBid + " kr \n");
                            }
                            System.out.println(fireBaseUsers.get(i).getUsername() + " with amount of " + latestBid);

                            Collections.reverse(allBids);
                            formattedBidArray = allBids.toString()
                                    .replace(",", "")  //remove the commas
                                    .replace("[", "")  //remove the left bracket
                                    .replace("]", "")  //remove the right bracket
                                    .trim();           //remove trailing spaces from partially initialized arrays
                            bidHistory.setText("     " + formattedBidArray + "\n");
                            bidHistory.autosize();
                            bidHistory.setWrapText(true);
                        }
                    }
                } catch (NullPointerException e) {
                    System.out.println("No bids..");
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("Firebase read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void clearGUI() {
        System.out.println("GUI cleared!");
        allBids.clear();
        highestBid.clear();
        bidHistory.clear();
        //countdown.clear();
        itemDescription.clear();
        setImageBid();
    }

    public void setImageBid(){
        //bild i paus
        newImage = new Image("https://trello-attachments.s3.amazonaws.com/5634c471f711dd19d0b7c708/233x283/c3d31b11bd646960821f58c32cc3f542/logo.PNG");
        itemImage.setImage(newImage);
        itemImage.setFitWidth(100);
        itemImage.setPreserveRatio(true);
        itemImage.setSmooth(true);
        itemImage.setCache(true);
    }

    public void updateDescription(BidItem item) {
        //omvandlar image string och sätter som imageview
        imageString = item.getImage();
        BASE64Decoder base64Decoder = new BASE64Decoder();
        try {
            ByteArrayInputStream decodedImage = new ByteArrayInputStream(base64Decoder.decodeBuffer(imageString));
            newImage = new Image(decodedImage);
            itemImage.setImage(newImage);
            itemImage.setFitWidth(450);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Set information to variable
        description = item.getDescription();
        type = item.getType();
        titleName = item.getTitle();
        itemId = item.getId();
        buyerId = item.getIdBuyer();
        seller="";
        String a = item.getIdSeller();
        System.out.println("item seller: " + a);
        for (int i = 0; i < fireBaseUsers.size(); i++){

            String b = fireBaseUsers.get(i).getId();
            System.out.println("searching match in user: " + b);
            if (a.equals(b)){
                seller = fireBaseUsers.get(i).getUsername();
                System.out.println("Corresponding seller for this item was found..: " + seller);
            }
        }
        startPrice = item.getStartedPrice();


        /*Information to TextBox*/
        itemDescription.setText(
                "Seller: " + seller + "\n" +
                        "Title: " + titleName + "\n" +
                        "Category: " + type + "\n" +
                        "Description: \n" + description + "\n\n" +
                        "Start price: " + startPrice + "\n"
        );

    }

    public void nextItem() {
        BidItem bidItem = fireBaseItems.get(0);
        seconds = TIME;
        activate.put("upForSale", true);
        activate.put("sold", false);
        latestBid=0;
        highestBidder=0;
        myFirebase.child(bidItem.getId()).updateChildren(activate);

        System.out.println("Updated Description!");
    }

    public void pastItemForSale() {
        BidItem bidItem = fireBaseItems.get(0);
        deactivate.put("upForSale", false);
//        deactivate.put("sold", true);
        myFirebase.child(bidItem.getId()).updateChildren(deactivate);

    }

    public void pastItemSold(){
        BidItem bidItem = fireBaseItems.get(0);
        deactivate.put("sold", true);
        myFirebase.child(bidItem.getId()).updateChildren(deactivate);
    }

    public void goOnline() {
        myFirebase.goOnline();
    }

    public void onStop(){
        myFirebase.goOffline();
    }

    public void buttonClicked(ActionEvent event) {

        System.out.println("Button was clicked, auction started!");
        nextItem();

    }

}
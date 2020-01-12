var dataInfo;

$.ajax({
        url: "http://localhost:3000/data",  // the local Node server
        method: 'GET',
        success: function(data){
            dataInfo = data; //display data in cosole to see if I receive it
        }
      });

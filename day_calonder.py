d=int(input("input date."))
m=int(input("input month.in no."))
y=int(input("input year."))

asq=[0,3,0,3,2,3,2,3,3,2,3,2,3]
a=0
for i in range(m):
    a += asq[i]

if(y%4==0 and m>2):
    a += 1    

x=(y-1)%400
if(x<100):
    fi=x//4
    u=x+fi+a+d
elif(x<200):
    fi=(x-100)//4
    u=(x-100-fi)+2*fi+d+a+5
elif(x<300):
    fi=(x-200)//4
    u=(x-200-fi)+2*fi+d+a+3
elif(x<400):
    fi=(x-300)//4
    u=(x-300-fi)+2*fi+d+a+1

day=u%7

seq=['1-Monday','2-Tuesday','3-Wednesday','4-Thursday','5-Friday','6-Saturday','7-Sunday']
print(seq[day-1])
input("Enter to exit")

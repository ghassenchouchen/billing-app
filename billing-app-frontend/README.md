# Frontend Project Structure





```
src/app/
├── core/                          
│   ├── guards/                    
│   │   └── auth.guard.ts        
│   ├── interceptors/             
│   │   ├── auth.interceptor.ts   
│   │   └── error.interceptor.ts 
│   ├── models/                    
│   │   └── index.ts              
│   └── services/                  
│       ├── api.service.ts        
│       └── auth.service.ts       
│
├── shared/                        
│   └── services/                  
│       ├── customer.service.ts
│       ├── bill.service.ts
│       ├── contract.service.ts
│       ├── services.service.ts
│       ├── offers.service.ts
│       ├── login.service.ts
│       └── index.ts              
│
├── bills/                         
│   ├── bills.component.ts
│   ├── bills.component.html
│   └── bills.component.css
│
├── contracts/                     
│   ├── contracts.component.ts
│   ├── contracts.component.html
│   └── contracts.component.css
│
├── customers/                     
│   ├── customers.component.ts
│   ├── customers.component.html
│   └── customers.component.css
│
├── login/                         
│   ├── login.component.ts
│   ├── login.component.html
│   └── login.component.css
│
├── offers/                        
│   ├── offers.component.ts
│   ├── offers.component.html
│   └── offers.component.css
│
├── services/                      
│   ├── services.component.ts
│   ├── services.component.html
│   └── services.component.css
│
├── app-routing.module.ts          
├── app.module.ts                  
├── app.component.ts               
└── app.component.html             
```









function generateData(num_data, x_dim, y_dim, satisfy_rate)


% generate planted 2-dnf
num_term = 4;
dnf = randi([-x_dim x_dim], num_term, 2);
while (nnz(dnf) ~= 2*num_term)
    dnf = randi([-x_dim x_dim], num_term, 2);
    for i = 1:num_term
        if (abs(dnf(i,1))==abs(dnf(i,2)))
            continue;
        end
    end
end

% generate X
satisfy = floor(num_data*satisfy_rate);
X = zeros(num_data, x_dim);

% generate Xs that satisfy the planted 2-dnf
for i = 1:satisfy
    while (true)
        X(i,:) = randi([0 1], 1,x_dim);
    if ((sgn(X(i,abs(dnf(1,1))))==sgn(dnf(1,1)) && sgn(X(i,abs(dnf(1,2))))==sgn(dnf(1,2))) || ...
        (sgn(X(i,abs(dnf(2,1))))==sgn(dnf(2,1)) && sgn(X(i,abs(dnf(2,2))))==sgn(dnf(2,2))) ||...
        (sgn(X(i,abs(dnf(3,1))))==sgn(dnf(3,1)) && sgn(X(i,abs(dnf(1,2))))==sgn(dnf(3,2))) ||...
        (sgn(X(i,abs(dnf(4,1))))==sgn(dnf(4,1)) && sgn(X(i,abs(dnf(1,2))))==sgn(dnf(4,2)))) 
            break;
    end
    end
end

% generate Xs that falsify the planted 2-dnf
for i = (satisfy+1):num_data
    while (true)
        X(i,:) = randi([0 1], 1,x_dim);
    if ((sgn(X(i,abs(dnf(1,1))))==sgn(dnf(1,1)) && sgn(X(i,abs(dnf(1,2))))==sgn(dnf(1,2))) || ...
        (sgn(X(i,abs(dnf(2,1))))==sgn(dnf(2,1)) && sgn(X(i,abs(dnf(2,2))))==sgn(dnf(2,2))) ||...
        (sgn(X(i,abs(dnf(3,1))))==sgn(dnf(3,1)) && sgn(X(i,abs(dnf(1,2))))==sgn(dnf(3,2))) ||...
        (sgn(X(i,abs(dnf(4,1))))==sgn(dnf(4,1)) && sgn(X(i,abs(dnf(1,2))))==sgn(dnf(4,2)))) == false
            break;
    end
    end
end


% generate parameter Mu & planted regression dimension
mu = normrnd(0, 0.1,satisfy,1);
b = 1;
planted_dim = randi(y_dim,1,2);

% generate the coefficient 
while (true)
    coeff = normrnd(0,0.1,1,2);
    if (norm(coeff)<= b) 
        break;
    end
end

% generate Y and Z
Y = unifrnd(-b,b, num_data,y_dim);
z_true = zeros(satisfy,1);

for i=1:satisfy
    row = Y(i,:);
    z_true(i) = coeff* row(planted_dim)' + mu(i);
end

z_false = normrnd(0,1,num_data-satisfy,1);
Z = [z_true; z_false];
data = [Y Z];

% shuffle data
newRowOrder = randperm(num_data);
new_X = X(newRowOrder, :);
new_YZ = data(newRowOrder, :);

% save data
csvwrite(strcat("YZ_m",num2str(num_data),".csv"), new_YZ);
csvwrite(strcat("X_m",num2str(num_data),".csv"), new_X);
csvwrite(strcat("DNF_m",num2str(num_data),".csv"), dnf);
csvwrite(strcat("dim_m",num2str(num_data),".csv"), planted_dim);
csvwrite(strcat("coeff_m",num2str(num_data),".csv"), coeff);
csvwrite(strcat("mu_m",num2str(num_data),".csv"), mu);
end

% helper function
function val = sgn(x)
if (sign(x) > 0)
    val = 1;
else
    val = -1;
end
end
package chaos.s18.a1o1;

import chaos.s18.a1o1.meta.C18SubMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Self-contained helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C18Sub", builder=C18Sub.C18SubBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C18Sub", model="chaos", builder=C18Sub.C18SubBuilderImpl.class, version="1.0.0")
public interface C18Sub extends RosettaModelObject {

	C18SubMeta metaData = new C18SubMeta();

	/*********************** Getter Methods  ***********************/
	String getS();

	/*********************** Build Methods  ***********************/
	C18Sub build();
	
	C18Sub.C18SubBuilder toBuilder();
	
	static C18Sub.C18SubBuilder builder() {
		return new C18Sub.C18SubBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C18Sub> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C18Sub> getType() {
		return C18Sub.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("s"), String.class, getS(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C18SubBuilder extends C18Sub, RosettaModelObjectBuilder {
		C18Sub.C18SubBuilder setS(String s);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("s"), String.class, getS(), this);
		}
		

		C18Sub.C18SubBuilder prune();
	}

	/*********************** Immutable Implementation of C18Sub  ***********************/
	class C18SubImpl implements C18Sub {
		private final String s;
		
		protected C18SubImpl(C18Sub.C18SubBuilder builder) {
			this.s = builder.getS();
		}
		
		@Override
		@RosettaAttribute("s")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("s")
		public String getS() {
			return s;
		}
		
		@Override
		public C18Sub build() {
			return this;
		}
		
		@Override
		public C18Sub.C18SubBuilder toBuilder() {
			C18Sub.C18SubBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C18Sub.C18SubBuilder builder) {
			ofNullable(getS()).ifPresent(builder::setS);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18Sub _that = getType().cast(o);
		
			if (!Objects.equals(s, _that.getS())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (s != null ? s.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C18Sub {" +
				"s=" + this.s +
			'}';
		}
	}

	/*********************** Builder Implementation of C18Sub  ***********************/
	class C18SubBuilderImpl implements C18Sub.C18SubBuilder {
	
		protected String s;
		
		@Override
		@RosettaAttribute("s")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("s")
		public String getS() {
			return s;
		}
		
		@RosettaAttribute("s")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("s")
		@Override
		public C18Sub.C18SubBuilder setS(String _s) {
			this.s = _s == null ? null : _s;
			return this;
		}
		
		@Override
		public C18Sub build() {
			return new C18Sub.C18SubImpl(this);
		}
		
		@Override
		public C18Sub.C18SubBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18Sub.C18SubBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getS()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18Sub.C18SubBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C18Sub.C18SubBuilder o = (C18Sub.C18SubBuilder) other;
			
			
			merger.mergeBasic(getS(), o.getS(), this::setS);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18Sub _that = getType().cast(o);
		
			if (!Objects.equals(s, _that.getS())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (s != null ? s.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C18SubBuilder {" +
				"s=" + this.s +
			'}';
		}
	}
}
